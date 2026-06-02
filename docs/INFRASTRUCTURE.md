# 인프라스트럭처

> AWS·Kubernetes 리소스 카탈로그와 Terraform 구성을 설명한다. 런타임 토폴로지·요청 흐름은 [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md), 백엔드 코드 구조는 [ARCHITECTURE.md](ARCHITECTURE.md), GitOps 배포 메커니즘은 [CLAUDE.md](../CLAUDE.md) `## CI/CD` 참조.

## 1. Terraform 레이아웃

```
infra/terraform/
├── main.tf         # provider, backend(S3+DynamoDB lock), 공통 데이터 소스
├── variables.tf    # 입력 변수 (project, aws_region, domain_name, github_org/repo, db_*, jwt_secret …)
├── outputs.tf      # 외부 노출값 (ECR URL, ACM ARN, github_actions_role_arn 등)
├── vpc.tf          # VPC, public/private/db 서브넷, NAT, RDS SG
├── eks.tf          # EKS 모듈, 노드 그룹, IRSA, 모든 helm_release(애드온 + Argo + bootstrap)
├── rds.tf          # RDS 모듈 (MySQL 8, Multi-AZ)
├── ecr.tf          # backend·frontend repo + lifecycle policy
├── secrets.tf      # Secrets Manager 컨테이너 (env·argocd)
├── acm.tf          # ALB용(ap-northeast-2) + CloudFront용(us-east-1) 와일드카드 인증서
├── cloudfront.tf   # CloudFront 배포 + Route53 alias 레코드
├── waf.tf          # CloudFront WAFv2 (us-east-1, scope=CLOUDFRONT) + 액세스 로그 S3
├── iam.tf          # GitHub Actions OIDC provider + 역할 + ECR 정책
└── monitoring.tf   # CloudWatch 로그 그룹·알람·대시보드
```

**상태 관리**: `s3://student-mgmt-tfstate` + DynamoDB lock(`student-mgmt-tfstate-lock`). 최초 apply 전 수동 생성 필요(`main.tf` 주석 참조).

**Provider 버전**:
| Provider | 제약 |
|---------|------|
| `hashicorp/aws` | `~> 5.50` (+ `aws.us_east_1` alias) |
| `hashicorp/kubernetes` | `~> 2.30` (exec auth: `aws eks get-token`) |
| `hashicorp/helm` | `~> 2.13` |
| `hashicorp/tls` | `~> 4.0` |

## 2. AWS 리소스 카탈로그

### 2.1 Networking — VPC
- CIDR `10.0.0.0/16`, 2 AZ(`ap-northeast-2`).
- **public 서브넷** `10.0.1.0/24, 10.0.2.0/24` — ALB 배치, `kubernetes.io/role/elb=1` 태그.
- **private 서브넷** `10.0.11.0/24, 10.0.12.0/24` — EKS 노드 그룹, `kubernetes.io/role/internal-elb=1` 태그.
- **db 전용 서브넷** `10.0.21.0/24, 10.0.22.0/24` — RDS 격리.
- NAT Gateway: `one_nat_gateway_per_az` (HA, 비용 절감 모드는 `single_nat_gateway`로 전환).
- RDS 보안그룹: EKS 노드 SG에서만 3306 ingress 허용.

### 2.2 Compute — EKS
- 클러스터 `student-mgmt-cluster`, k8s 1.30, public endpoint(필요 시 `cluster_endpoint_public_access_cidrs`로 제한).
- 노드 그룹 `student-mgmt-nodegroup`: `t3.medium` ON_DEMAND, **min 2 / desired 2 / max 5**, 50GB gp3 EBS.
- 클러스터 애드온: `coredns`, `kube-proxy`, `vpc-cni`(NetworkPolicy 강제, IRSA), `aws-ebs-csi-driver`(IRSA).
- `enable_cluster_creator_admin_permissions = true` (Terraform 실행 주체에 클러스터 admin).
- **GitHub Actions에는 클러스터 권한 없음**(2025 GitOps 전환).

### 2.3 Data — RDS MySQL
- `db.t3.medium`, MySQL 8.0, Multi-AZ, allocated 20GB(Auto-Scaling 100GB 상한).
- Parameter group: `utf8mb4`, `Asia/Seoul` 타임존.
- 백업: `backup_retention_period: 7`, `Mon:04:00-Mon:05:00` 유지보수창.
- `deletion_protection: true`, `skip_final_snapshot: false`, `final_snapshot_identifier_prefix`.
- Performance Insights 활성(7일 보존).
- 환경별로 단일 인스턴스 안에 논리 DB 3개(`student_mgmt_{dev,staging,prod}`) — 컷오버 시 SQL로 1회 생성.

### 2.4 Container Registry — ECR
- repo `student-mgmt/backend`, `student-mgmt/frontend` (각각 별도).
- `image_tag_mutability: IMMUTABLE` — 같은 태그 덮어쓰기 금지(GitOps 결정론성).
- `scan_on_push: true` — 푸시 시 취약점 스캔.
- 라이프사이클: untagged 1일 후 만료, 전체 최신 15개 유지.

### 2.5 Edge — CloudFront + WAF + ACM + Route53
- **ACM**(ap-northeast-2): `<domain>` + `*.<domain>` 와일드카드 — ALB·Argo Ingress 공용.
- **ACM**(us-east-1): 동일 와일드카드 — CloudFront 전용(CloudFront ACM은 반드시 us-east-1).
- DNS 검증 레코드는 Route53 호스팅 존(`var.domain_name`)에 자동 생성, ap-northeast-2 검증 fqdn을 us-east-1에서 공유.
- **CloudFront 배포** `aliases = [<domain>, www.<domain>]`, Origin = `alb.<domain>`(external-dns가 ALB 호스트네임으로 자동 생성):
  - `/api/*` → 캐시 없음, 모든 헤더·쿠키 forward.
  - `/assets/*` → 1일 default / 1년 max, query/cookie 미포함, compress.
  - 기본 → HTML 0초, Authorization/Origin/Host 헤더 forward.
- **WAF**(us-east-1, `scope=CLOUDFRONT`): `AWSManagedRulesCommonRuleSet` + `AWSManagedRulesKnownBadInputsRuleSet`, CloudWatch 메트릭 활성.
- **Route53**: `<domain>`·`www.<domain>` A(Alias) → CloudFront. `argocd.<domain>`·`dev/staging/app.<domain>`은 external-dns가 Ingress 어노테이션 보고 자동 관리.
- CloudFront 액세스 로그 → 전용 S3 버킷 `${project}-cf-logs-${account}`(30일 만료, ACL=private).

### 2.6 Secrets Manager
| 시크릿 | 용도 | 채움 시점 |
|--------|------|-----------|
| `student-mgmt/db` | 단일 환경 DB 자격증명 (기존) | Terraform `_version`으로 자동 |
| `student-mgmt/jwt` | 단일 환경 JWT 시크릿 (기존) | Terraform `_version`으로 자동 |
| `student-mgmt/{dev,staging,prod}/db` | 환경별 DB 자격증명·jdbc_url | **수동**(컷오버 시 `put-secret-value`) |
| `student-mgmt/{dev,staging,prod}/jwt` | 환경별 JWT 시크릿 | **수동** |
| `student-mgmt/argocd/admin` | Argo CD admin bcrypt 비밀번호 + `passwordMtime` | **수동** |
| `student-mgmt/argocd/repo` | Argo CD private repo 자격증명 `{url,username,token}` | **수동** |

ESO IRSA 정책은 `student-mgmt/*` 전체를 허용하므로 새 시크릿 추가 시 IAM 변경 불필요.

### 2.7 Observability — CloudWatch
- 로그 그룹:
  - `/aws/eks/student-mgmt-cluster/cluster` (30일) — EKS 컨트롤 플레인.
  - `/aws/eks/student-mgmt-cluster/application` (14일) — Fluent Bit이 컨테이너 stdout 수집.
- RDS 알람: CPU > 80%(2주기), DatabaseConnections > 80(1주기), FreeStorageSpace < 5GB(1주기).
- 대시보드 `student-mgmt`: 위 3개 RDS 지표 시계열.
- 클러스터 내부 메트릭은 kube-prometheus-stack(아래 §3) — CloudWatch와 분리.

## 3. EKS 클러스터 애드온 (Terraform `helm_release`)

`eks.tf`에 다음 helm_release가 정의됨. 각각 IRSA로 최소 권한 AWS 호출.

| 이름 | 차트 | 네임스페이스 | 책임 | IRSA |
|------|------|------------|------|------|
| `aws_lbc` | aws-load-balancer-controller | kube-system | Ingress → ALB 프로비저닝 | `student-mgmt-aws-lbc` |
| `external_secrets` | external-secrets/external-secrets | external-secrets | ESO 컨트롤러 | `student-mgmt-external-secrets` (정책: `student-mgmt/*`) |
| `cluster_autoscaler` | autoscaler/cluster-autoscaler | kube-system | 노드 그룹 자동 확장 | `student-mgmt-cluster-autoscaler` |
| `metrics_server` | metrics-server | kube-system | HPA 메트릭 소스 | (없음) |
| `external_dns` | external-dns | kube-system | Ingress → Route53 레코드 | `student-mgmt-external-dns` (hosted zone arn 제한) |
| `aws_for_fluent_bit` | aws-for-fluent-bit | kube-system | 컨테이너 로그 → CloudWatch | `student-mgmt-fluentbit` |
| `kube_prometheus_stack` | prometheus-community/kube-prometheus-stack | monitoring | Prometheus + Grafana + Alertmanager + CRDs(ServiceMonitor 등) | (없음, 클러스터 내) |
| `argocd` | argo/argo-cd (`var.argocd_chart_version`, 기본 7.7.7) | argocd | GitOps 컨트롤러 + UI | (없음) |
| `argocd_bootstrap` | `${path.module}/../../k8s/bootstrap` (로컬 차트) | argocd | AppProject, ApplicationSet, ClusterSecretStore, Argo ESO(admin·repo) 생성 | (없음) |

**Argo CD 차트 값 요지**(`eks.tf`):
- `configs.params."server.insecure" = true` — ALB가 TLS 종료, server↔ALB는 HTTP(이중 TLS 방지).
- `configs.cm."resource.customizations.health.external-secrets.io_ExternalSecret"` — Lua 헬스체크로 ExternalSecret Ready 상태 게이팅(sync wave 0).
- `server.ingress`: `argocd.<domain>`, ALB internet-facing, ACM 와일드카드, `group.name=argocd`(앱 ALB와 분리), external-dns 호스트.
- `depends_on = [module.eks, helm_release.aws_lbc]`.

**`argocd_bootstrap` 로컬 차트**(`k8s/bootstrap/`):
- AppProject `student-mgmt` (소스 레포 + 3개 네임스페이스로 제한).
- ApplicationSet `student-mgmt` (list 제너레이터 → `student-mgmt-{env}` Application × 3, path=`k8s/overlays/{{.env}}`, syncPolicy automated + `CreateNamespace=true`, `ServerSideApply=true`).
- ClusterSecretStore `aws-secrets-manager` (region 주입).
- ExternalSecret `argocd-admin`(target `argocd-secret`, `creationPolicy: Merge`) — 차트가 만든 시크릿에 admin 키만 머지.
- ExternalSecret `argocd-repo`(target `argocd-repo-student-mgmt`, `Owner`, label `argocd.argoproj.io/secret-type: repository`).

## 4. IAM 모델

### 4.1 GitHub Actions OIDC
- OIDC provider: `https://token.actions.githubusercontent.com`.
- 역할 `student-mgmt-github-actions`:
  - Trust: `repo:${github_org}/${github_repo}:ref:refs/heads/main` + `refs/tags/v*` 만 assume 가능.
  - 정책: **`AmazonEC2ContainerRegistryPowerUser` 한 개만**. EKS 권한·`eks:DescribeCluster` **없음**.
- 결과: CI는 ECR push만 가능, 클러스터 apply 권한 0. GitOps 전환의 보안 핵심.

### 4.2 IRSA (서비스 어카운트 ↔ IAM 역할)
| 역할 | 사용처 (SA) | 권한 요약 |
|------|------------|----------|
| `student-mgmt-vpc-cni` | `kube-system:aws-node` | AmazonEKS_CNI_Policy (IPv4) |
| `student-mgmt-ebs-csi` | `kube-system:ebs-csi-controller-sa` | EBS CSI |
| `student-mgmt-aws-lbc` | `kube-system:aws-load-balancer-controller` | ALB 생성·관리 |
| `student-mgmt-external-secrets` | `external-secrets:external-secrets` | `secretsmanager:GetSecretValue/DescribeSecret` on `student-mgmt/*` |
| `student-mgmt-cluster-autoscaler` | `kube-system:cluster-autoscaler` | ASG describe/modify (이 클러스터 한정) |
| `student-mgmt-external-dns` | `kube-system:external-dns` | Route53 (호스팅 존 ARN 한정) |
| `student-mgmt-fluentbit` | `kube-system:aws-for-fluent-bit` | CloudWatch Logs put |

모든 IRSA는 EKS OIDC provider를 trust하며 namespace+SA 조합으로 분리.

### 4.3 EKS access entry
- `enable_cluster_creator_admin_permissions = true` — Terraform 실행 주체 admin.
- `access_entries`: **비어 있음**(과거에 있던 GitHub Actions 어드민 부여는 GitOps 전환으로 제거).

## 5. 환경별 분리 매트릭스

| 자원 | 환경별 분리 | 공유 | 비고 |
|------|------------|------|------|
| EKS 클러스터·노드 그룹 | — | 공유 | 단일 클러스터 |
| 네임스페이스 | `student-mgmt-{env}` | — | Argo Application 단위 |
| ALB | `group.name: student-mgmt-{env}` | — | Ingress 어노테이션으로 분리 |
| ACM 인증서 | 공유 와일드카드 | — | 1개 인증서로 모든 서브도메인 |
| Route53 호스트 | `{dev/staging/app}.<domain>` | — | external-dns가 자동 생성 |
| RDS 인스턴스 | — | 공유 | 비용 절감 |
| RDS DB 스키마 | `student_mgmt_{env}` | — | 1회 SQL로 생성 |
| Secrets Manager | `student-mgmt/{env}/db,jwt` | — | ESO IRSA가 `student-mgmt/*` 전체 허용 |
| ECR | — | 공유 | 환경별 태그(promote 흐름)로 분리 |
| Argo Application | `student-mgmt-{env}` | — | ApplicationSet list 제너레이터 |

## 6. Terraform 부트스트랩 순서

```bash
# 1) 사전 수동 생성 (1회)
aws s3 mb s3://student-mgmt-tfstate --region ap-northeast-2
aws dynamodb create-table --table-name student-mgmt-tfstate-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST --region ap-northeast-2

# 2) terraform.tfvars 작성 (domain_name, github_org, db_password, jwt_secret …)

# 3) EKS 먼저 생성 (provider exec auth는 클러스터가 존재해야 토큰 발급 가능)
terraform apply -target=module.eks

# 4) 전체 apply — 애드온(helm_release) + Argo CD + bootstrap 차트 한 번에
terraform apply
```

이후 컷오버 절차(시크릿 값 주입, RDS DB 스키마 생성, Argo 상태 확인)는 [Task 11 운영 런북](superpowers/plans/2026-05-28-argocd-gitops.md#task-11) 참조.

## 7. 변경·확장 시 체크리스트

- **새 환경 추가**: `k8s/overlays/<env>/` overlay 3개 파일, `k8s/bootstrap/values.yaml`의 `environments`에 추가, `secrets.tf`의 `for_each = toset([...])`에 추가, 호스트 결정(`<env>.<domain>`), Secrets Manager 값 주입.
- **새 도메인**: `acm.tf`(ap-northeast-2 + us-east-1) + `cloudfront.tf` aliases + Route53.
- **새 ECR repo**: `ecr.tf`에 추가, IAM 정책 재검토(현재 PowerUser는 광범위 — 운영 강화 시 repo 한정 정책 고려).
- **새 helm 애드온**: `eks.tf` 끝에 `helm_release` 추가, 필요 시 IRSA 모듈 추가, `depends_on`으로 부트스트랩 순서 명시.
- **노드 인스턴스 타입 변경**: `var.eks_node_instance_type` 수정 → 노드 그룹 롤링 교체(PDB·HPA가 무중단 보장).

## 8. 비용·운영 주의사항

- 가장 큰 비용: EKS 컨트롤 플레인 + 노드 그룹(2×t3.medium) + NAT Gateway(AZ당 1) + RDS(`db.t3.medium` Multi-AZ).
- 비용 절감 옵션: `single_nat_gateway=true`, RDS Multi-AZ 비활성(가용성↓), 노드 그룹 SPOT 혼합(현재 ON_DEMAND).
- `image_tag_mutability: IMMUTABLE`이라 같은 SHA 재푸시는 실패(예상 동작) — GitOps 결정론성을 보장하기 위함.
- `deletion_protection: true`라 RDS는 단순 `terraform destroy`로 삭제되지 않음(콘솔에서 보호 해제 후 삭제 필요).
- CloudFront 가격 클래스 미지정(전 세계). 한국·아시아만 서비스 시 `price_class` 추가로 비용 절감 가능.
