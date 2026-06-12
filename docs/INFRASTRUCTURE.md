# 인프라스트럭처

> AWS·Kubernetes 리소스 카탈로그와 Terraform 구성을 설명한다. 런타임 토폴로지·요청 흐름은 [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md), 백엔드 코드 구조는 [ARCHITECTURE.md](ARCHITECTURE.md), GitOps 배포 메커니즘은 [CLAUDE.md](../CLAUDE.md) `## CI/CD` 참조.

## 1. Terraform 레이아웃

```
infra/terraform/
├── main.tf         # provider 설정, S3+DynamoDB backend, 공통 데이터 소스
├── variables.tf    # 입력 변수 (project, aws_region, github_org/repo, db_*, jwt_secret, amplify_github_token, sealed_secrets_chart_version, argocd_chart_version …)
├── outputs.tf      # 외부 노출값 (ECR URL, github_actions_role_arn, amplify_app_id, amplify_branch_urls, apigateway_endpoints, …)
├── vpc.tf          # VPC, public/private/db 서브넷, NAT, RDS SG
├── eks.tf          # EKS 모듈, 노드 그룹, IRSA, 모든 애드온 + Argo + Sealed Secrets helm_releases
├── rds.tf          # RDS MySQL 8 Multi-AZ
├── ecr.tf          # backend ECR repo + lifecycle policy
├── amplify.tf      # Amplify 앱 + 브랜치 × 3 (커스텀 도메인 없음, 기본 amplifyapp.com 사용)
├── apigateway.tf   # HTTP API × 3 + VPC Link × 3 + 통합 + ALB SG 룰 (기본 execute-api URL 사용)
└── iam.tf          # GitHub Actions OIDC provider + 역할(ECR 전용)
```

**상태 관리**: `s3://student-mgmt-tfstate` + DynamoDB lock(`student-mgmt-tfstate-lock`). 최초 apply 전 수동 생성 필요(`main.tf` 주석 참조).

**Provider 버전:**

| Provider | 제약 |
|---------|------|
| `hashicorp/aws` | `~> 5.50` (us-east-1 alias는 더 이상 사용 안 함, 향후 main.tf에서 제거 가능) |
| `hashicorp/kubernetes` | `~> 2.30` (exec auth: `aws eks get-token`) |
| `hashicorp/helm` | `~> 2.13` |
| `hashicorp/tls` | `~> 4.0` |

## 2. AWS 리소스 카탈로그

### 2.1 Networking — VPC
- CIDR `10.0.0.0/16`, 2 AZ(`ap-northeast-2`).
- **public 서브넷** `10.0.1.0/24, 10.0.2.0/24` — ALB(internal로 변경됐지만 서브넷 태그는 그대로), `kubernetes.io/role/elb=1`.
- **private 서브넷** `10.0.11.0/24, 10.0.12.0/24` — EKS 노드 + API Gateway VPC Link ENI, `kubernetes.io/role/internal-elb=1`.
- **db 전용 서브넷** `10.0.21.0/24, 10.0.22.0/24` — RDS 격리.
- NAT Gateway: `one_nat_gateway_per_az` (HA).
- RDS SG: EKS 노드 SG의 3306만 허용.

### 2.2 Compute — EKS
- 클러스터 `student-mgmt-cluster`, k8s 1.30, public endpoint(필요 시 `cluster_endpoint_public_access_cidrs`로 제한).
- 노드 그룹: `t3.medium` ON_DEMAND, **min 2 / desired 2 / max 5**, 50GB gp3.
- 클러스터 애드온: `coredns`, `kube-proxy`, `vpc-cni`(IRSA), `aws-ebs-csi-driver`(IRSA).
- `enable_cluster_creator_admin_permissions = true` (Terraform 실행 주체 admin).
- **GitHub Actions에는 클러스터 권한 없음.**

### 2.3 Data — RDS MySQL
- `db.t3.medium`, MySQL 8.0, Multi-AZ, 20GB(Auto-Scaling 100GB 상한).
- **`storage_encrypted = true`** — 디스크 평문 저장 금지(기본 AWS 관리 KMS 키, 개인정보보호법 정렬).
- 파라미터: `utf8mb4`, `Asia/Seoul` 타임존.
- 백업 7일, `Mon:04:00-Mon:05:00` 유지보수창.
- `deletion_protection: true`, Performance Insights 활성(7일).
- 단일 인스턴스 + 환경별 논리 DB 3개(`student_mgmt_{dev,staging,prod}`) — 컷오버 시 SQL로 1회 생성.
- **스키마 마이그레이션**: Flyway(`src/main/resources/db/migration/V*__*.sql`). prod 프로파일은 `ddl-auto: validate`로 잠겨 있어 Hibernate 자동 ALTER 불가. 첫 prod 배포 전 `schemadump` 프로파일로 V1__init.sql 1회 생성 필요.

### 2.4 Container Registry — ECR
- repo `student-mgmt/backend` 1개만(frontend는 Amplify로 이관).
- `image_tag_mutability: IMMUTABLE` — GitOps 결정론성.
- `scan_on_push: true`.
- 라이프사이클: untagged 1일 후 만료, 전체 최신 15개 유지.

### 2.5 Edge — Amplify + API Gateway (AWS 기본 도메인)
**커스텀 도메인 없음.** ACM 인증서·Route53 호스팅 존 미사용. AWS가 자동 발급/관리하는 기본 도메인 사용:
- **Amplify**: 단일 앱(`aws_amplify_app.main`), 3개 브랜치(`develop` / `staging` / `main`). 브랜치별 SPA URL = `https://<branch>.<app-id>.amplifyapp.com`. TLS·CDN·인증서 Amplify가 자동 관리. URL 확인: `terraform output amplify_branch_urls`.
- **API Gateway HTTP API × 3** + VPC Link × 3:
  - 환경마다 별도 API + VPC Link + 통합. **커스텀 도메인 매핑 없음** — 기본 invoke URL(`https://<api-id>.execute-api.ap-northeast-2.amazonaws.com`)을 그대로 사용.
  - 통합 대상: `data "aws_lb_listener" "env_http"`로 환경의 private ALB(80) listener arn을 조회 → HTTP_PROXY + VPC_LINK.
  - 모든 경로 `ANY /{proxy+}`로 패스스루.
  - CORS allow_origins: 해당 환경 Amplify 기본 URL 1개만 허용.
  - **Throttling**: stage `default_route_settings`에 rate 100 req/s, burst 200 — DoS·비용 폭증 방어선.
  - URL 확인: `terraform output apigateway_endpoints`.

### 2.6 시크릿 — Sealed Secrets
- `helm_release "sealed_secrets"` (Bitnami chart `sealed-secrets`, namespace `sealed-secrets`).
- 컨트롤러가 자체 비대칭 키를 생성·보관(`kubectl get secret -n sealed-secrets -l sealedsecrets.bitnami.com/sealed-secrets-key`).
- 운영자가 `kubeseal` CLI로 환경별 시크릿 암호화 → `k8s/overlays/<env>/sealed-secrets/*.yaml`로 git 커밋.
- Argo sync → controller가 SealedSecret을 일반 Secret으로 materialize → Pod `envFrom` 소비.
- **AWS Secrets Manager 미사용**(제거됨).

### 2.7 관측 — Prometheus 스택
- `helm_release "kube_prometheus_stack"` (`monitoring` namespace): Prometheus + Grafana + Alertmanager + ServiceMonitor CRDs.
- backend `ServiceMonitor`가 `/actuator/prometheus` 30초 간격 스크랩.
- UI 접근: port-forward 전용. 공용 노출 없음.
- **CloudWatch 로그 그룹·알람·대시보드 미사용**(제거됨). 로그는 `kubectl logs`로.

## 3. EKS 클러스터 애드온 (Terraform `helm_release`)

| 이름 | 차트 | 네임스페이스 | 책임 | IRSA |
|------|------|------------|------|------|
| `aws_lbc` | aws-load-balancer-controller | kube-system | Ingress → 환경별 private ALB 자동 생성 | `student-mgmt-aws-lbc` |
| `cluster_autoscaler` | autoscaler/cluster-autoscaler | kube-system | 노드 그룹 자동 확장 | `student-mgmt-cluster-autoscaler` |
| `metrics_server` | metrics-server | kube-system | HPA 메트릭 소스 | (없음) |
| `kube_prometheus_stack` | prometheus-community/kube-prometheus-stack | monitoring | Prometheus + Grafana + Alertmanager + CRDs (Discord 알람 라우팅, Loki 데이터소스) | (없음) |
| `loki` | grafana/loki | monitoring | 로그 집계 저장소 (SingleBinary, 20Gi PV, 7일 보존) | (없음) |
| `promtail` | grafana/promtail | monitoring | 노드 로그 수집 → Loki push (DaemonSet) | (없음) |
| `sealed_secrets` | bitnami-labs/sealed-secrets | sealed-secrets | SealedSecret 복호화 컨트롤러 | (없음) |
| `argocd` | argo/argo-cd | argocd | GitOps 컨트롤러 + UI | (없음) |
| `argocd_bootstrap` | `${path.module}/../../k8s/bootstrap` (로컬 차트) | argocd | AppProject + ApplicationSet | (없음) |

**제거된 helm_release**(이전 설계 잔재): `external_secrets`(ESO), `aws_for_fluent_bit`, `external_dns`(커스텀 도메인 미사용). 관련 IAM policy `eso_secrets_manager`와 IRSA `eso_irsa`, `fluentbit_irsa`, `external_dns_irsa`도 삭제.

**`argocd_bootstrap` 로컬 차트**(`k8s/bootstrap/`):
- AppProject `student-mgmt` (소스 레포 + 3개 대상 네임스페이스로 제한).
- ApplicationSet `student-mgmt` (list 제너레이터 → `student-mgmt-{env}` Application × 3, path=`k8s/overlays/{{.env}}`, syncPolicy automated + `CreateNamespace=true`, `ServerSideApply=true`).
- ClusterSecretStore·argocd-admin/repo ExternalSecret 템플릿은 제거됨(ESO 미사용).

## 4. IAM 모델

### 4.1 GitHub Actions OIDC
- OIDC provider: `https://token.actions.githubusercontent.com`.
- 역할 `student-mgmt-github-actions`:
  - Trust: `repo:${github_org}/${github_repo}:ref:refs/heads/main` + `refs/tags/v*`만 assume 가능.
  - 정책: **`AmazonEC2ContainerRegistryPowerUser` 한 개만**. EKS 권한·`eks:DescribeCluster`·Secrets Manager 없음.
- 결과: CI는 ECR push + manifest write-back만 가능. 클러스터 apply 권한 0. GitOps 보안 핵심.

### 4.2 IRSA (서비스 어카운트 ↔ IAM 역할)

| 역할 | 사용처 (SA) | 권한 요약 |
|------|------------|----------|
| `student-mgmt-vpc-cni` | `kube-system:aws-node` | AmazonEKS_CNI_Policy |
| `student-mgmt-ebs-csi` | `kube-system:ebs-csi-controller-sa` | EBS CSI |
| `student-mgmt-aws-lbc` | `kube-system:aws-load-balancer-controller` | ALB 생성·관리 |
| `student-mgmt-cluster-autoscaler` | `kube-system:cluster-autoscaler` | ASG describe/modify (이 클러스터 한정) |

모든 IRSA는 EKS OIDC provider를 trust하며 namespace+SA 조합으로 분리.

**제거된 IRSA**: `eso_irsa`(ESO 미사용), `fluentbit_irsa`(Fluent Bit 미사용), `external_dns_irsa`(external-dns 미사용).

### 4.3 EKS access entry
- `enable_cluster_creator_admin_permissions = true` — Terraform 실행 주체 admin.
- `access_entries`: 비어 있음(GitHub Actions에 클러스터 권한 없음).

## 5. 시크릿 모델

### 5.1 흐름
```
[운영자 로컬]
   1. 평문 Secret YAML 생성 (kubectl create secret --dry-run=client)
   2. kubeseal --controller-namespace sealed-secrets로 암호화
   3. SealedSecret YAML을 k8s/overlays/{env}/sealed-secrets/에 커밋

[클러스터]
   4. Argo CD sync → SealedSecret 리소스 적용
   5. sealed-secrets-controller가 자체 비대칭 키로 복호화
   6. 일반 Secret 리소스 materialize (Owner: SealedSecret)
   7. backend Pod의 envFrom: secretRef로 환경변수 주입
```

### 5.2 시크릿 키 관리·백업
컨트롤러가 부트스트랩 시 자동 생성한 비대칭 키는 `sealed-secrets` 네임스페이스 Secret에 보관(라벨 `sealedsecrets.bitnami.com/sealed-secrets-key=active`). 클러스터를 재생성하면 키도 새로 만들어지므로, **운영자가 별도 안전 저장소에 백업 필수**:
```bash
kubectl get secret -n sealed-secrets -l sealedsecrets.bitnami.com/sealed-secrets-key \
  -o yaml > ~/secure-backup/sealed-secrets-key-$(date +%F).yaml
```

## 6. 환경별 분리 매트릭스

| 자원 | 환경별 분리 | 공유 | 비고 |
|------|------------|------|------|
| EKS 클러스터·노드 그룹 | — | 공유 | 단일 클러스터 |
| 네임스페이스 | `student-mgmt-{env}` | — | Argo Application 단위 |
| ALB | aws-lbc가 환경별 1개 생성 | — | `group.name: student-mgmt-{env}` |
| API Gateway HTTP API | 환경별 1개 | — | `aws_apigatewayv2_api.env[env]` |
| VPC Link | 환경별 1개 | — | 환경별 보안그룹 |
| API URL | 환경별 AWS 기본 invoke URL | — | `<api-id>.execute-api.<region>.amazonaws.com` |
| Amplify 브랜치 | develop/staging/main | — | 단일 앱, 브랜치만 분기 |
| SPA URL | 환경별 Amplify 기본 URL | — | `<branch>.<app-id>.amplifyapp.com` |
| RDS 인스턴스 | — | 공유 | 비용 절감 |
| RDS 논리 DB | `student_mgmt_{env}` | — | 1회 SQL로 생성 |
| ECR | — | 공유 | `student-mgmt/backend` 한 repo, 태그로 분리 |
| Argo Application | `student-mgmt-{env}` | — | ApplicationSet list 제너레이터 |
| SealedSecret | `k8s/overlays/{env}/sealed-secrets/` | — | 환경별 별도 파일 |

## 7. Terraform 부트스트랩 순서 (2-phase)

API Gateway가 `data "aws_lb"`로 ALB를 조회하는데, ALB는 Argo가 Ingress를 sync한 뒤에야 생성된다. 따라서 첫 apply는 API Gateway를 제외하고, ALB가 생긴 뒤 두 번째 apply로 마무리한다.

```bash
# 1) 사전 수동 생성 (1회)
aws s3 mb s3://student-mgmt-tfstate --region ap-northeast-2
aws dynamodb create-table --table-name student-mgmt-tfstate-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST --region ap-northeast-2

# 2) terraform.tfvars: domain_name, github_org, github_repo, db_password,
#    jwt_secret, amplify_github_token (GitHub PAT)

# 3) EKS 먼저
terraform apply -target=module.eks

# 4) Phase 1: 애드온·Sealed Secrets·Argo·Amplify (API Gateway 제외)
terraform apply -target=helm_release.aws_lbc \
                -target=helm_release.cluster_autoscaler \
                -target=helm_release.metrics_server \
                -target=helm_release.kube_prometheus_stack \
                -target=helm_release.sealed_secrets \
                -target=helm_release.argocd \
                -target=helm_release.argocd_bootstrap \
                -target=aws_amplify_app.main \
                -target=aws_amplify_branch.env \
                -target=aws_amplify_domain_association.main

# 5) 운영자가 kubeseal로 SealedSecret 생성·커밋, RDS 논리 DB 생성
#    Argo가 Ingress sync → 환경별 private ALB 생성됨

# 6) Phase 2: 전체 apply (API Gateway 포함)
terraform apply
```

자세한 컷오버 절차는 [Task 21 운영 런북](superpowers/plans/2026-06-03-system-redesign.md#task-21) 참조.

## 8. 변경·확장 체크리스트

- **새 환경 추가**: `k8s/overlays/<env>/` overlay 3종 + `sealed-secrets/`, `k8s/bootstrap/values.yaml`의 `environments`에 추가, `infra/terraform/apigateway.tf`의 `local.apigw_envs`에 추가, `infra/terraform/amplify.tf`의 `local.amplify_branches`에 추가, 신규 Amplify 브랜치 생성, 시크릿 값 주입.
- **커스텀 도메인 추가(추후)**: `acm.tf`로 ACM 인증서 재추가 + Route53 호스팅 존 데이터 소스 + `aws_amplify_domain_association` + `aws_apigatewayv2_domain_name` + Route53 alias 레코드 추가. 현재는 미사용으로 인프라 단순화.
- **새 helm 애드온**: `eks.tf` 끝에 `helm_release` 추가, 필요 시 IRSA 모듈 추가, `depends_on`으로 부트스트랩 순서 명시.
- **노드 인스턴스 타입 변경**: `var.eks_node_instance_type` 수정 → 노드 그룹 롤링 교체(PDB·HPA가 무중단 보장).

## 9. 비용·운영 주의사항

- **가장 큰 비용**: EKS 컨트롤 플레인 + 노드 그룹(2×t3.medium) + NAT Gateway(AZ당 1) + RDS(`db.t3.medium` Multi-AZ) + Amplify(빌드 분/호스팅 요청). API Gateway는 요청량 과금이라 저트래픽에서 거의 무료.
- **비용 절감 옵션**: `single_nat_gateway=true`, RDS Multi-AZ 비활성(가용성↓), 노드 그룹 SPOT 혼합(현재 ON_DEMAND), 환경 수 축소(staging 제거).
- **CloudFront 미사용**: SPA는 Amplify CDN, API는 API Gateway가 직접 처리. 둘 다 자체 TLS·캐싱 제공.
- **이미지 불변성**: `image_tag_mutability: IMMUTABLE`라 같은 SHA 재푸시는 실패(예상 동작 — GitOps 결정론성).
- **`deletion_protection: true`**라 RDS는 단순 `terraform destroy`로 삭제되지 않음(콘솔에서 보호 해제 후 삭제 필요).
- **Sealed Secrets 키 분실 시**: 모든 SealedSecret을 다시 만들어야 함(원본 평문이 필요). 백업 필수.
- **2-phase apply 의존성**: 첫 apply 후 Argo sync 완료를 기다리지 않고 두 번째 apply를 하면 `data "aws_lb"` resolve 실패. Task 21 런북 절차 준수.

---

## 10. 학습 분석(EP-08) 인프라 — **계획 / 미구현**

> [SYSTEM_ARCHITECTURE.md §9](SYSTEM_ARCHITECTURE.md#9-학습-분석-ep-08-계획--미구현), [BACKLOG.md `EP-08`](../BACKLOG.md) 참조. 본 섹션은 EP-08 구현 시 추가될 인프라를 미리 정리. 별도 세션에서 작업.

### 10.1 필수 — 분석 DB + 스케줄러 ETL

추가 AWS 리소스 **0건**. 모두 기존 RDS·EKS 안에서 처리.

| 항목 | 변경 |
|------|------|
| RDS 논리 DB | 환경별 `student_mgmt_analytics_{env}` 1회 SQL로 생성(`CREATE DATABASE ...`). 컷오버 런북에 추가. |
| Spring Boot | `application.yml`에 분석 DB datasource 추가 (`spring.datasource.analytics.*`), `@EnableScheduling`로 ETL 컴포넌트 실행 |
| SealedSecret | `backend-secrets`에 분석 DB 자격증명 추가(또는 운영과 동일 사용자 사용) |
| 모니터링 | ETL 실패율·지연 시간을 Prometheus 메트릭으로 노출 (`/actuator/prometheus`에 자동 포함) |

### 10.2 가점 — Kafka 기반 CDC

**옵션 A: AWS MSK Serverless (관리형, 권장)**
- `aws_msk_serverless_cluster` 리소스 신규
- 비용: 처리량·스토리지 기반 종량제(저트래픽 학습용 프로젝트라면 월 $50~100 수준 가능)
- IAM 인증 사용(IRSA로 backend Pod에 `kafka-cluster:*` 권한 부여)
- 추가 Terraform: `infra/terraform/msk.tf` 신규

**옵션 B: Strimzi (자체 호스팅, EKS 내부)**
- `helm_release "strimzi_operator"` + Kafka·KafkaConnect Custom Resource
- 추가 AWS 리소스 0건, 단 노드 그룹·EBS 스토리지 부담 증가
- 비용 ↓, 운영 복잡도 ↑(Kafka 설정·운영 직접 책임)

**공통**:
- Debezium MySQL Connector를 Kafka Connect에서 실행 → 운영 DB binlog 읽기
- 운영 DB MySQL 파라미터: `binlog_format=ROW`, `binlog_row_image=FULL` 필요 → `rds.tf`의 `parameters`에 추가
- Kafka Connect JDBC Sink 또는 Spring Kafka Consumer가 분석 DB에 적재
- backend NetworkPolicy에 Kafka 통신 허용

### 10.3 선택 — AI 챗봇

| 항목 | 추가/변경 |
|------|----------|
| LLM 제공자 | Claude(Anthropic API), OpenAI, 또는 AWS Bedrock 중 택1. Bedrock 선택 시 IRSA로 IAM 권한 부여 가능 (AWS 외부 API는 단순 키 인증) |
| API 키 | SealedSecret `backend-secrets`에 `LLM_API_KEY` 추가 |
| 백엔드 | `ChatController`, `ChatService` 신규. 분석 DB 조회 도구를 function calling으로 노출(권한별 데이터 범위 제한) |
| 프론트엔드 | 채팅 UI 컴포넌트, SSE/streaming 응답 처리 |
| API Gateway | streaming 응답 위해 HTTP API의 `payload_format_version`을 확인(2.0이면 SSE OK), 필요 시 `/api/chat` 라우트를 long-timeout으로 분리 |
| 비용 | LLM API 토큰 사용량 종량제. Bedrock(Claude)은 영문 기준 $3/M input tokens 수준. 학생당 월 수십 회 질의면 매우 저렴 |

### 10.4 EP-08 추가 시 부트스트랩 보강

기존 [§7 2-phase apply](#7-terraform-부트스트랩-순서-2-phase) 절차에 다음 추가:
- Phase 1과 Phase 2 사이 또는 Phase 2 이후 분석 DB `CREATE DATABASE student_mgmt_analytics_{env}` 1회 실행
- (옵션 B Strimzi 선택 시) `helm_release "strimzi_operator"`를 Phase 1에 포함 → Argo가 KafkaCluster CR sync → Connect Cluster 기동
- (옵션 A MSK 선택 시) `aws_msk_serverless_cluster`는 Phase 1에서 생성 가능(ALB 의존 없음)
- LLM API 키 SealedSecret을 운영자가 kubeseal로 추가
