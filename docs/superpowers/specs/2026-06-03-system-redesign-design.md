# 시스템 전면 재설계: Amplify + API Gateway + EKS GitOps

> 작성일: 2026-06-03 · 대상: 교사용 학생 성적/상담 관리 시스템
> 상태: 설계 승인됨, 구현 계획 작성 전
> 이전 시도: [2026-05-28-argocd-gitops-design.md](2026-05-28-argocd-gitops-design.md) — Secrets Manager·CloudFront·WAF·ESO 기반 설계. 사용 의도 미스매치로 폐기, 본 문서가 대체함.

## 1. 목표

스택을 사용자가 실제로 채택할 11개 기술로 한정하고, 그에 맞는 런타임 토폴로지·CI/CD·시크릿 흐름을 처음부터 다시 정의한다.

**확정 스택**: Amplify · API Gateway · ALB · EKS · RDS · VPC · Prometheus · Docker · Kubernetes · Argo CD · Terraform.

**제거 대상**: AWS Secrets Manager, ESO, CloudFront, WAF, Fluent Bit, CloudWatch 알람/대시보드, 클러스터 내 nginx 프론트엔드.

## 2. 확정 결정사항

| # | 항목 | 선택 | 근거 |
|---|------|------|------|
| 1 | 진입점 토폴로지 | Amplify(SPA) + API Gateway HTTP API(public) + private ALB → EKS | AWS-native 표준, ALB 인터넷 비노출 |
| 2 | 인증 책임 | Spring Security (JWT 검증·RBAC) | 기존 로직 재사용, 도입 비용 0 |
| 3 | 시크릿 | Sealed Secrets controller | 완전 GitOps, AWS 서비스 추가 없음 |
| 4 | 환경 | dev / staging / prod | 표준 3-env |
| 5 | API Gateway 개수 | 환경별 별도 API 3개 | blast radius 분리, 도메인 매핑 단순 |
| 6 | 도메인 | SPA: `<domain>` / `dev.<domain>` / `staging.<domain>` · API: `api.<domain>` / `api-dev.<domain>` / `api-staging.<domain>` | prod는 루트, dev/staging은 서브도메인 |
| 7 | Argo UI | port-forward 전용 | 공용 노출 0, 부트스트랩 admin 비밀번호로 충분 |
| 8 | 로그 | kubectl logs / Pod stdout (Loki는 추후) | YAGNI, Prometheus만으로 메트릭 충분 |

## 3. As-Is (현재 상태)

`feature/argocd-gitops` 브랜치 19 커밋 + main 기준 인프라:

**살릴 자산** (재사용):
- VPC(2 AZ, public/private/db subnets), EKS module(k8s 1.30), 노드 그룹
- RDS MySQL (Multi-AZ, db.t3.medium), ECR(`student-mgmt/backend`, `/frontend`)
- IAM/OIDC: GitHub Actions 역할(ECR PowerUser만), IRSA 인프라
- Helm releases: `aws_lbc`, `cluster_autoscaler`, `metrics_server`, `external_dns`, `kube_prometheus_stack`, `argocd`
- Kustomize base+overlays 구조(매니페스트는 갈아엎되 디렉토리/명명 규칙은 유지)
- Argo bootstrap chart (AppProject, ApplicationSet — 본문만 갈아엎음)
- `.github/workflows/cd.yml`의 ECR push + write-back 패턴, `promote.yml`
- 이번 세션에서 만든 Kustomize 구조·CI 권한 분리 정책

**버릴 자산** (제거):
- `infra/terraform/cloudfront.tf`, `waf.tf`, `monitoring.tf`(CloudWatch alarms/dashboard/log groups)
- `infra/terraform/secrets.tf`의 모든 `aws_secretsmanager_secret` 리소스
- `aws_for_fluent_bit` helm_release + IRSA(`fluentbit_irsa`)
- `eso_irsa` + `aws_iam_role_policy "eso_secrets_manager"`, `helm_release "external_secrets"`
- `k8s/base/external-secrets/` 전체
- `k8s/bootstrap/templates/clustersecretstore.yaml`, `argocd-admin-externalsecret.yaml`, `argocd-repo-externalsecret.yaml`
- `k8s/base/frontend/` 전체 (nginx Deployment/Service/HPA/PDB)
- 와일드카드 ACM(us-east-1) — CloudFront 미사용으로 불필요
- ECR `student-mgmt/frontend` repo (Amplify가 빌드/배포)

**신규**:
- `infra/terraform/amplify.tf` — Amplify 앱 + 브랜치(× 3) + 도메인 연결
- `infra/terraform/apigateway.tf` — HTTP API × 3 + VPC Link × 3 + 통합 + 커스텀 도메인 + Route53 alias
- `infra/terraform/sealed_secrets.tf` (또는 eks.tf에 추가) — Sealed Secrets controller helm_release
- 환경별 SealedSecret YAML 파일(kubeseal로 운영자가 생성, `k8s/overlays/{env}/sealed-secrets/`)
- Ingress 어노테이션: `alb.ingress.kubernetes.io/scheme: internal`로 변경

## 4. To-Be 한 화면 토폴로지

```
                  사용자 (브라우저)
                       │
        ┌──────────────┴──────────────┐
        │ SPA 자산                     │ API 호출
        │ https://<host>               │ https://api[-env].<domain>
        ▼                              ▼
   ┌─────────────┐             ┌────────────────────┐
   │ AWS Amplify  │             │ API Gateway HTTP   │
   │ React build  │             │ API (환경별 3개)    │
   │ + CDN + TLS  │             │ TLS·throttling     │
   │ branch=env   │             │ dumb passthrough   │
   └─────────────┘             └─────────┬──────────┘
                                          │ VPC Link
                                          ▼
                          ┌──────────────────────────┐
                          │ private ALB (scheme:     │
                          │ internal, 환경별 1개)     │
                          │ aws-load-balancer-       │
                          │ controller가 관리         │
                          └────────┬──────────────────┘
                                   │ HTTP
                          ┌────────┴──────────────────┐
                          │ EKS (백엔드 전용)          │
                          │                            │
                          │ student-mgmt-{env} ns:     │
                          │   backend Deployment       │
                          │   Service / HPA / PDB      │
                          │   Ingress(scheme:internal) │
                          │   SealedSecret → Secret    │
                          │   ServiceMonitor           │
                          │                            │
                          │ argocd ns:                 │
                          │   Argo CD + ApplicationSet │
                          │ sealed-secrets ns:         │
                          │   controller               │
                          │ monitoring ns:             │
                          │   kube-prometheus-stack    │
                          └────────┬──────────────────┘
                                   │ JDBC
                                   ▼
                          ┌──────────────────┐
                          │ RDS MySQL         │
                          │ (Multi-AZ, 단일   │
                          │  인스턴스 + 논리   │
                          │  DB 3개)          │
                          └──────────────────┘
```

## 5. 상세 설계

### 5.1 도메인 & TLS

| 환경 | SPA (Amplify) | API (API Gateway) |
|------|---------------|---------------------|
| prod | `<domain>` (루트) | `api.<domain>` |
| staging | `staging.<domain>` | `api-staging.<domain>` |
| dev | `dev.<domain>` | `api-dev.<domain>` |

- **ACM 인증서**: ap-northeast-2 와일드카드 `*.<domain>` + 루트 `<domain>` 1장 — ALB·API Gateway·Amplify 공유. CloudFront 미사용으로 us-east-1 인증서 불필요.
- **Route53**: 단일 호스팅 존 `<domain>` 사용. Amplify 도메인 연결은 콘솔 또는 `aws_amplify_domain_association` 리소스로 자동, API Gateway 커스텀 도메인은 `aws_api_gateway_domain_name` + `aws_route53_record` alias.

### 5.2 Amplify

- **단일 Amplify 앱**, 브랜치 매핑:
  - branch `main` → prod 환경, 도메인 `<domain>`, env `VITE_API_URL=https://api.<domain>`
  - branch `staging` → staging 환경, `staging.<domain>`, `VITE_API_URL=https://api-staging.<domain>`
  - branch `develop` → dev 환경, `dev.<domain>`, `VITE_API_URL=https://api-dev.<domain>`
- **빌드 설정**(`amplify.yml` in repo):
  ```yaml
  version: 1
  applications:
    - appRoot: frontend
      frontend:
        phases:
          preBuild:
            commands: [npm install]
          build:
            commands: [npm run build]
        artifacts:
          baseDirectory: dist
          files: ['**/*']
        cache:
          paths: [node_modules/**/*]
  ```
- **CI**: Amplify Console이 GitHub webhook으로 자동 빌드/배포. GitHub Actions에는 프론트 빌드 워크플로우 불필요.
- **GitHub 연결**: 최초 1회 콘솔에서 OAuth 권한 부여. 토큰은 Amplify가 보관.

### 5.3 API Gateway × 3 (환경별 HTTP API)

각 환경마다 다음 리소스 셋:
- `aws_apigatewayv2_api` (HTTP API)
- `aws_apigatewayv2_vpc_link` — private 서브넷에 ENI 생성, 자신의 보안그룹
- `aws_apigatewayv2_integration` (HTTP_PROXY, `connection_type: VPC_LINK`, `integration_uri: <ALB listener ARN>`)
- `aws_apigatewayv2_route` `ANY /{proxy+}` — 모든 경로 백엔드로 패스스루
- `aws_apigatewayv2_stage` `$default` (자동 배포)
- `aws_apigatewayv2_domain_name` (custom domain)
- `aws_apigatewayv2_api_mapping`
- `aws_route53_record` (A alias → API Gateway regional domain)

**Terraform 패턴**: `for_each = toset(["dev","staging","prod"])`로 3 인스턴스 생성, 각자 자기 env의 ALB listener를 가리킴.

**ALB Listener ARN 획득**: aws-load-balancer-controller가 만든 ALB는 Ingress에서 동적으로 생성됨 → Terraform이 ARN을 직접 알기 어려움. 해결: Ingress 생성 후 `data "aws_lb"` data source로 태그/이름 기반 조회.

### 5.4 EKS 워크로드

**네임스페이스**: `student-mgmt-{dev,staging,prod}` · `argocd` · `sealed-secrets` · `monitoring`

**환경별 워크로드**(student-mgmt-{env}):
```
backend Deployment (Spring Boot :8080)
backend Service (ClusterIP)
backend HPA (CPU 70%, mem 80%, 2–10)
backend PDB (minAvailable: 1)
NetworkPolicy (backend pod ← ALB SG CIDR만, frontend pod 제거)
Ingress (group.name: student-mgmt-{env}, scheme: internal)
SealedSecret backend-credentials (DB + JWT)
ServiceMonitor (port: http, path: /actuator/prometheus)
```

**제거된 리소스**(이전 설계 대비):
- frontend Deployment/Service/HPA/PDB → Amplify로 이관
- ExternalSecret → SealedSecret으로 교체
- ClusterSecretStore → 불필요(Sealed Secrets는 클러스터 스코프 자체 키)

### 5.5 Sealed Secrets

- **Controller**: `bitnami-labs/sealed-secrets` chart, `sealed-secrets` 네임스페이스. Terraform `helm_release "sealed_secrets"`.
- **키 관리**:
  - 컨트롤러가 클러스터 부트스트랩 시 비대칭 키쌍 자동 생성(`active` 라벨).
  - 운영자가 `kubectl get secret -n sealed-secrets -l sealedsecrets.bitnami.com/sealed-secrets-key -o yaml > backup.yaml`로 백업(클러스터 재생성 시 복원).
- **운영자 워크플로우**:
  ```bash
  # 1) 평문 Secret 생성(로컬, git 커밋 금지)
  kubectl create secret generic backend-credentials \
    -n student-mgmt-dev \
    --from-literal=DB_USERNAME=appuser \
    --from-literal=DB_PASSWORD=... \
    --from-literal=SPRING_DATASOURCE_URL=... \
    --from-literal=JWT_SECRET=... \
    --dry-run=client -o yaml > /tmp/secret.yaml

  # 2) kubeseal로 암호화
  kubeseal --controller-namespace sealed-secrets \
    --format yaml < /tmp/secret.yaml \
    > k8s/overlays/dev/sealed-secrets/backend-credentials.yaml

  # 3) git 커밋 (암호화된 SealedSecret만)
  ```
- **Pod consumption**: backend Deployment의 `envFrom: secretRef: { name: backend-credentials }`.

### 5.6 GitOps (Argo CD)

- **ApplicationSet** (이전 설계와 동일 패턴): list 제너레이터로 `student-mgmt-{env}` Application × 3 생성. `path: k8s/overlays/{{.env}}`.
- **AppProject**: 소스 레포 + 3개 대상 네임스페이스 + `sealed-secrets`, `monitoring` 리소스 권한.
- **Argo UI**: 차트의 server.ingress는 비활성. 노출 0. 관리자 접근:
  ```bash
  kubectl port-forward svc/argocd-server -n argocd 8080:443
  # https://localhost:8080
  ```
  초기 admin 비밀번호: `kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d`
- **bootstrap chart**: 이전 설계의 ClusterSecretStore·argocd-admin/repo ExternalSecret 템플릿 삭제. AppProject + ApplicationSet만 남김.

### 5.7 CI/CD 흐름

**프론트엔드** (Amplify가 전담):
```
git push (frontend/** 변경)
   │
   ▼
Amplify Console (GitHub webhook)
   │
   ▼
branch별 빌드 → 환경별 배포 (자동)
```

**백엔드** (이전과 동일, frontend 부분만 삭제):
```
git push main (k8s/**, frontend/** 제외)
   │
   ▼
GitHub Actions cd.yml
   ├─ ECR push: backend:${github.sha}  (OIDC, ECR PowerUser만)
   └─ write-back: kustomize edit set image on k8s/overlays/dev
       → GITHUB_TOKEN으로 main 커밋·push
   │
   ▼
Argo CD git watch → student-mgmt-dev Application 자동 sync
```

**승격**(`promote.yml` 그대로): workflow_dispatch → 환경 간 이미지 태그 복사 PR → 머지 = 게이트.

**`paths-ignore`**: `k8s/**`, `docs/**`, `frontend/**` (frontend는 Amplify가 처리).

### 5.8 관측

- **메트릭**: `kube_prometheus_stack` helm_release(monitoring ns). Prometheus + Grafana + Alertmanager + CRDs.
- **앱 메트릭 수집**: backend ServiceMonitor (`/actuator/prometheus`, 30초 간격).
- **Grafana**: port-forward 전용 (`kubectl port-forward svc/...grafana 3000:80 -n monitoring`).
- **로그**: 별도 수집 인프라 없음. `kubectl logs -n student-mgmt-{env} <pod>` 또는 `k9s`. Pod 재시작 시 이전 로그 손실 — 운영 강화 시 Loki 추가 검토(별도 결정).
- **알람**: Prometheus Alertmanager 활용. AWS CloudWatch 알람 미사용.

### 5.9 보안 경계

| 경계 | 메커니즘 |
|------|---------|
| 외부 → Amplify | TLS (ACM, Amplify 관리) |
| 외부 → API Gateway | TLS, throttling, custom domain |
| API Gateway → ALB | VPC Link (private subnet ENI), VPC 내부 통신 |
| ALB ↔ Pod | HTTP, 노드 SG가 ALB SG만 허용 |
| Pod ↔ RDS | RDS SG가 노드 SG에서 3306만 허용 |
| Pod ↔ Pod | NetworkPolicy(backend ← ALB CIDR만) |
| 시크릿 git 저장 | Sealed Secrets (KMS 비대칭, cluster key) |
| API 인증 | Spring Security JWT (15분 access + 7일 refresh) |
| CI → AWS | GitHub OIDC, ECR PowerUser만 (클러스터 권한 0) |
| CI → 클러스터 | 없음 (Argo가 유일 apply 주체) |
| Argo UI | port-forward only (공용 노출 0) |

## 6. 파일별 변경 요약

**생성**:
- `infra/terraform/amplify.tf` — `aws_amplify_app`, `aws_amplify_branch` × 3, `aws_amplify_domain_association`, GitHub access token(GH PAT을 var로 받음)
- `infra/terraform/apigateway.tf` — HTTP API × 3 (`for_each`), VPC Link × 3, integration·route·stage·domain·mapping·route53 record
- `infra/terraform/sealed_secrets.tf` — `helm_release "sealed_secrets"`
- `amplify.yml` (repo root 또는 `frontend/amplify.yml`)
- `k8s/overlays/{env}/sealed-secrets/backend-credentials.yaml` — 운영자가 kubeseal로 생성(컷오버 단계)

**수정**:
- `infra/terraform/eks.tf` — `eso_irsa`, `helm_release "external_secrets"`, `aws_for_fluent_bit`, `fluentbit_irsa` 제거. ALB SG 입력(VPC Link SG) 허용.
- `infra/terraform/iam.tf` — `aws_iam_role_policy "eso_secrets_manager"` 제거. GitHub Actions OIDC trust는 그대로(ECR만).
- `infra/terraform/acm.tf` — us-east-1 인증서·검증 리소스 제거(CloudFront 미사용).
- `infra/terraform/ecr.tf` — `student-mgmt/frontend` repo + lifecycle 제거.
- `infra/terraform/variables.tf` — `argocd_chart_version` 유지, `amplify_github_token`, `sealed_secrets_chart_version` 등 추가.
- `infra/terraform/outputs.tf` — `amplify_default_domain`, `apigateway_endpoint_per_env` 등 추가; CloudFront 관련 제거.
- `k8s/base/external-secrets/` — 폴더 삭제.
- `k8s/base/frontend/` — 폴더 삭제.
- `k8s/base/kustomization.yaml` — frontend·external-secrets 리소스 항목 제거, sealed-secrets는 overlay에서 추가하므로 base는 backend·ingress·network-policy만.
- `k8s/base/ingress.yaml` — backend 단독 경로(`/api`, `/swagger-ui`, `/v3/api-docs`)로 단순화, `alb.ingress.kubernetes.io/scheme: internal` 추가.
- `k8s/base/network-policy.yaml` — frontend 정책 제거, backend 정책의 frontend pod selector 제거.
- `k8s/overlays/{env}/kustomization.yaml` — frontend image 항목 제거, `sealed-secrets/backend-credentials.yaml` resources 추가, `externalsecret-patch.yaml` 삭제.
- `k8s/overlays/{env}/ingress-patch.yaml` — 호스트 어노테이션 제거(API Gateway가 DNS 처리; external-dns 어노테이션도 제거), cert ARN 어노테이션은 ALB가 내부지만 TLS 유지 시 그대로 또는 제거(아래 5.6.3 참조)
- `k8s/bootstrap/templates/` — `clustersecretstore.yaml`, `argocd-admin-externalsecret.yaml`, `argocd-repo-externalsecret.yaml` 삭제. AppProject + ApplicationSet만 남김.
- `k8s/bootstrap/values.yaml` — repoURL·environments 유지, region 제거 가능.
- `.github/workflows/cd.yml` — frontend 빌드/푸시 스텝 제거, `paths-ignore`에 `frontend/**` 추가, ECR write-back은 backend만.
- `CLAUDE.md` / `AGENTS.md` / `docs/SYSTEM_ARCHITECTURE.md` / `docs/INFRASTRUCTURE.md` — Amplify·API Gateway·Sealed Secrets 기반으로 전면 재작성.

**삭제 후보**:
- `infra/terraform/cloudfront.tf`
- `infra/terraform/waf.tf`
- `infra/terraform/monitoring.tf` (CloudWatch alarms/dashboard 전부)
- `infra/terraform/secrets.tf` 전부 (Secrets Manager 미사용)
- `k8s/monitoring/servicemonitor.yaml`은 유지(kube-prometheus-stack가 사용)

## 7. 마이그레이션 절차 개요

1. 19 커밋 분석 → 살릴 자산만 별도 브랜치로 cherry-pick하거나, 현 브랜치에서 "remove + add" 커밋 시퀀스로 진행(브랜치 처리 방침은 별도 결정).
2. Terraform: cloudfront/waf/secrets/monitoring 파일 삭제 → amplify·apigateway·sealed_secrets 파일 추가 → `terraform plan` 검증.
3. Kustomize: frontend·external-secrets 디렉토리 삭제, base/overlay 어노테이션 조정.
4. Amplify: 콘솔에서 앱 생성·GitHub 연결, 3 브랜치·도메인 매핑.
5. 컷오버 단계(라이브):
   - `terraform apply` (EKS·ALB·API Gateway·Sealed Secrets controller까지)
   - 운영자가 환경별 시크릿을 `kubeseal`로 암호화 후 커밋
   - Argo가 sync → 워크로드 기동
   - API Gateway custom domain ↔ Route53 ↔ 클라이언트(브라우저)에서 e2e 확인

## 8. 가정 및 범위 외

**가정**:
- 단일 EKS 클러스터, 환경은 네임스페이스로 분리.
- RDS 1개 + 논리 DB 3개 (이전 결정 유지).
- prod·staging·dev sync 모두 자동(승격 PR 머지가 게이트).
- Argo CD admin은 초기 비밀번호로 운영(SSO 미적용).

**범위 외(이번에는 안 함)**:
- Loki/Promtail 로그 수집
- GitHub SSO (Argo Dex), Cognito 도입
- API Gateway WAF (AWS WAFv2를 HTTP API에 부착 가능하지만 이번 스코프 외)
- API Gateway 인증(JWT authorizer) — 추후 Spring에서 분리하고 싶을 때 검토
- Multi-region/Multi-cluster
- Amplify 외 정적 호스팅(S3+CloudFront)
- 향후 reports 기능 위한 S3 버킷 (이전 설계의 reports bucket은 이미 제거됨)

## 9. 검증 방법

**정적**:
- `terraform fmt -check`, `terraform validate`
- `kubectl kustomize k8s/overlays/{env}` 세 환경 모두 렌더 성공
- `helm template` for bootstrap·sealed-secrets·argocd 차트
- `python -c "yaml.safe_load(...)" .github/workflows/*.yml`
- 매니페스트에 평문 시크릿 잔존 없음(`grep -rE "password|secret" k8s/`로 점검 — SealedSecret만 OK)

**런타임**(컷오버 후):
- Argo Application 3개 Synced + Healthy
- 각 환경 Ingress가 만든 ALB가 `scheme: internal`인지 콘솔에서 확인
- API Gateway custom domain에 curl `https://api-dev.<domain>/api/health` 200 OK
- Amplify dev 브랜치 빌드 결과를 `dev.<domain>`에서 로드 + SPA가 `api-dev.<domain>` 호출 (CORS·인증 동작)
- SealedSecret이 Secret으로 materialize됐는지 `kubectl get secret -n student-mgmt-dev backend-credentials`
- Prometheus가 backend 메트릭 스크랩 중인지 Grafana에서 확인
- CI 워크플로우(main push) → ECR push + write-back 커밋 + Argo dev sync 일순환 성공

**보안**:
- GitHub Actions 역할에 EKS·Secrets Manager 권한 0 (`aws iam get-role-policy` / `list-attached-role-policies`)
- ALB가 인터넷에서 직접 접근 불가능 (curl 시 timeout)
- Argo UI 인터넷에서 접근 불가능 (port-forward만 가능)
