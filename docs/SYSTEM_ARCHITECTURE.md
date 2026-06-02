# 시스템 아키텍처

> 본 문서는 **배포된 시스템의 런타임 토폴로지**를 다룬다. 백엔드 코드 내부 구조(레이어/패키지/인증 처리)는 [ARCHITECTURE.md](ARCHITECTURE.md)를, AWS 리소스 카탈로그·IAM·Terraform 구성은 [INFRASTRUCTURE.md](INFRASTRUCTURE.md)를 참조한다.

## 1. 한 화면 요약

```
                            ┌─────────────────────────┐
                            │   사용자 (교사/학생/학부모) │
                            └────────────┬────────────┘
                                         │ HTTPS  (https://<domain>)
                                         ▼
                              ┌───────────────────┐
                              │   AWS CloudFront   │  ← WAF (CommonRuleSet + KnownBadInputs)
                              │  (TLS, CDN, /assets│    us-east-1, scope=CLOUDFRONT
                              │   캐싱)            │
                              └─────────┬─────────┘
                                        │ HTTPS  (Origin: alb.<domain>)
                                        ▼
                              ┌───────────────────┐
                              │    AWS ALB         │  ← ACM 와일드카드 인증서
                              │ (internet-facing)  │    aws-load-balancer-controller
                              └─────────┬─────────┘
                                        │ HTTP
                ┌───────────────────────┴────────────────────────┐
                │     EKS 클러스터 (student-mgmt-cluster, k8s 1.30)│
                │                                                │
                │  argocd ns                                    │
                │   ├─ Argo CD (server/repo/applicationset)     │  → git watch
                │   └─ argocd-bootstrap (AppProject/AppSet/CSS) │
                │                                                │
                │  student-mgmt-{dev,staging,prod}              │
                │   ├─ Ingress  (group.name 별 ALB 분리)        │
                │   ├─ frontend Deployment (React/nginx :80)     │
                │   ├─ backend  Deployment (Spring Boot :8080)   │
                │   ├─ ExternalSecret → backend-secrets Secret   │
                │   ├─ HPA, PDB, NetworkPolicy, ServiceMonitor   │
                │                                                │
                │  kube-system ns (애드온)                       │
                │   aws-lbc, external-dns, cluster-autoscaler,   │
                │   metrics-server, ESO, fluent-bit, kube-       │
                │   prometheus-stack                             │
                └─────────┬──────────────────────┬──────────────┘
                          │ JDBC(SSL=false)      │ HTTPS API
                          ▼                      ▼
                ┌──────────────────┐   ┌───────────────────┐
                │  RDS MySQL 8     │   │ AWS Secrets Manager│
                │  Multi-AZ, t3.med│   │ student-mgmt/{env} │
                │  schemas:        │   │   /db, /jwt        │
                │  student_mgmt_   │   │ student-mgmt/argocd│
                │  {dev,stg,prod}  │   │   /admin, /repo    │
                └──────────────────┘   └───────────────────┘
```

## 2. 환경 분리 모델 (단일 클러스터 + 네임스페이스)

| 환경 | 네임스페이스 | 호스트 | ALB group.name | Secrets 경로 | DB 스키마 | Argo Application |
|------|-------------|--------|----------------|--------------|----------|------------------|
| dev | `student-mgmt-dev` | `dev.<domain>` | `student-mgmt-dev` | `student-mgmt/dev/*` | `student_mgmt_dev` | `student-mgmt-dev` (auto-sync) |
| staging | `student-mgmt-staging` | `staging.<domain>` | `student-mgmt-staging` | `student-mgmt/staging/*` | `student_mgmt_staging` | `student-mgmt-staging` (auto-sync, PR gate) |
| prod | `student-mgmt-prod` | `app.<domain>` | `student-mgmt-prod` | `student-mgmt/prod/*` | `student_mgmt_prod` | `student-mgmt-prod` (auto-sync, PR gate) |

- **공유**: EKS 클러스터·노드 그룹·RDS 인스턴스·ECR·ACM 와일드카드·CloudFront·WAF·애드온.
- **분리**: 네임스페이스(워크로드)·ALB 그룹·Ingress 호스트·Secrets Manager 경로·RDS 논리 DB·Argo Application.
- **격리 강도**: 가벼움(공유 클러스터/DB). prod blast-radius 강화가 필요하면 클러스터 분리 또는 RDS 분리로 진화.

## 3. 컴포넌트별 책임

### 3.1 프론트엔드 (`frontend/`)
- React 18 + Vite, nginx로 정적 서빙(`:80`).
- `/api/*` 호출은 백엔드 Service로 라우팅(같은 Ingress 호스트, 경로 분기).
- 정적 자산(`/assets/*`)은 CloudFront에서 최대 캐싱.

### 3.2 백엔드 (`src/main/java/com/studentmanagement/`)
- Spring Boot 3.3 / Java 21, `:8080`.
- 상세 코드 레이어/패키지·인증 흐름은 [ARCHITECTURE.md](ARCHITECTURE.md) 참조.
- 시크릿은 환경변수로 주입(`envFrom: secretRef: backend-secrets`). 평문 비밀번호는 매니페스트/이미지에 절대 들어가지 않음.
- 메트릭: `/actuator/prometheus` 엔드포인트 → `ServiceMonitor`가 30초 간격 스크랩.

### 3.3 데이터 (RDS MySQL)
- `db.t3.medium`, Multi-AZ, 자동 백업 7일, Performance Insights on, deletion protection.
- 단일 인스턴스 + 환경별 논리 DB(`student_mgmt_{env}`). 환경별 자격증명 분리(Secrets Manager).
- ESO IRSA가 Secrets Manager에서 풀링 → Kubernetes Secret으로 동기화 → Pod envFrom.

### 3.4 시크릿 (Secrets Manager + ESO)
```
AWS Secrets Manager                    ESO (cluster)                     Pod
─────────────────────                  ──────────────                    ──────
student-mgmt/<env>/db          ──┐
   {username,password,jdbc_url} │     ClusterSecretStore
student-mgmt/<env>/jwt         ──┼──▶ aws-secrets-manager  ──▶ Secret  ──▶ envFrom
   {secret}                     │     (IRSA: eso_irsa)        backend-
student-mgmt/argocd/admin      ──┤                            secrets
   {passwordBcrypt, *Mtime}     │
student-mgmt/argocd/repo       ──┘
   {url, username, token}
```
- ESO `ExternalSecret`은 `refreshInterval: 1h`로 회전 반영. Argo 시크릿(admin·repo)은 `argocd` 네임스페이스.

### 3.5 트래픽 진입 (CloudFront + ALB + WAF)
- CloudFront: 사용자 ↔ 시스템 사이 단일 TLS 종료점. `viewer_protocol_policy: redirect-to-https`.
  - `/api/*` 캐시 없음(쿠키/헤더 전부 forward). `/assets/*` 최대 캐싱.
- WAF(us-east-1, scope=CLOUDFRONT): `AWSManagedRulesCommonRuleSet` + `AWSManagedRulesKnownBadInputsRuleSet`.
- ALB(`aws-load-balancer-controller`): Ingress 어노테이션에서 `group.name`을 환경별로 달리 지정해 환경마다 ALB 분리. ACM 와일드카드(`*.<domain>`) 사용.
- external-dns: Ingress의 `external-dns.alpha.kubernetes.io/hostname` 어노테이션을 보고 Route53에 alb 레코드를 자동 생성.

### 3.6 관측 (Observability)
- **로그**: Fluent Bit DaemonSet → CloudWatch Log Group(`/aws/eks/student-mgmt-cluster/application`).
- **메트릭**: kube-prometheus-stack(Prometheus + Grafana) 클러스터 내. 백엔드 `ServiceMonitor`가 `/actuator/prometheus`를 30초 간격 스크랩.
- **AWS 알람·대시보드**: RDS CPU/Connections/FreeStorage 알람 + CloudWatch 대시보드(`student-mgmt`).
- **EKS 컨트롤 플레인 로그**: `/aws/eks/student-mgmt-cluster/cluster`.

## 4. 요청 흐름 (end-to-end)

```
1. 브라우저  ──HTTPS──▶ CloudFront (TLS 종료, WAF 평가, 정적 자산 캐시 hit 시 즉시 응답)
2. CloudFront ──HTTPS──▶ ALB (alb.<domain>)
3. ALB ──HTTP──▶ Ingress 규칙 매칭
       /api, /swagger-ui, /v3/api-docs  →  Service backend:8080
       그 외 (정적/SPA)                  →  Service frontend:80
4. Service ──ClusterIP──▶ Pod (NetworkPolicy로 podSelector 허용된 트래픽만)
5. backend Pod ──JDBC──▶ RDS (보안그룹: EKS 노드 SG만 3306 허용)
6. backend 응답 ──▶ ALB ──▶ CloudFront ──▶ 브라우저
```

## 5. 배포 흐름 (GitOps)

```
개발자 ── git push (main, k8s/ 제외 변경) ──▶ GitHub Actions cd.yml
                                                │
                                          ┌─────┴─────┐
                                          ▼           ▼
                                     ECR push    write-back commit
                              (backend, frontend  (kustomize edit set image
                               :<github.sha>)      on k8s/overlays/dev,
                                                   GITHUB_TOKEN으로 main push)
                                                          │
                                                          ▼
                                          Argo CD (in-cluster) git watch
                                                          │
                                          ┌───────────────┼───────────────┐
                                          ▼               ▼               ▼
                                  student-mgmt-dev  staging          prod
                                  (자동 sync)       (수동 PR 머지 게이트)
```

**승격(promote.yml)**: `workflow_dispatch(from=dev|staging, to=staging|prod)` → 원본 overlay의 핀된 이미지를 대상 overlay로 복사 → PR 생성. PR 머지가 환경 승격 게이트.

**Sync wave**: ExternalSecret(0) → Deployment/Service/HPA/PDB/NetworkPolicy(1) → Ingress(2). ExternalSecret의 Ready 상태는 Argo `resource.customizations` Lua 헬스체크로 게이팅(wave 0이 끝나야 wave 1 시작).

CI 권한 모델: GitHub Actions 역할은 **ECR PowerUser만** 보유 — 클러스터 자격증명 없음. 모든 apply 권한은 Argo CD(클러스터 내부 RBAC).

## 6. 보안 경계

| 계층 | 메커니즘 |
|------|---------|
| Edge (외부 → CloudFront) | TLS 1.2+, WAF(Managed Rules), `viewer_protocol_policy=https-only/redirect-to-https` |
| CloudFront ↔ ALB | HTTPS 전용(`origin_protocol_policy: https-only`), TLSv1.2 |
| ALB ↔ Pod | HTTP(인-VPC), 타깃 그룹 IP 타입 |
| Pod ↔ Pod | NetworkPolicy로 frontend → backend, ALB CIDR(10.0.0.0/16) → backend/frontend만 허용 |
| Pod ↔ RDS | RDS SG가 EKS 노드 SG에서만 3306 허용 |
| API 인증 | JWT (Access 15분 / Refresh 7일), `@PreAuthorize` 메서드 단위 |
| 시크릿 | Secrets Manager + ESO(IRSA `student-mgmt-external-secrets`, 정책 `student-mgmt/*`만 접근) |
| CI ↔ AWS | GitHub OIDC, 역할 분기(`repo:<org>/<repo>:ref:refs/heads/main` + `:ref:refs/tags/v*`), 정책 = ECR PowerUser만 |
| CI ↔ 클러스터 | **없음** (GitOps 전환의 핵심) |
| 클러스터 → AWS | IRSA(애드온별 최소 권한 IAM 역할, OIDC trust) |

## 7. 가용성·확장

- **Multi-AZ**: VPC 2 AZ, EKS 노드 그룹·NAT(`one_nat_gateway_per_az`)·RDS Multi-AZ 스탠바이.
- **무중단 배포**: RollingUpdate(`maxSurge:1, maxUnavailable:0`), readiness/liveness probe(backend `/api/health`, frontend `/healthz`), `terminationGracePeriodSeconds: 60`(Spring graceful shutdown).
- **PDB**: backend·frontend 각 `minAvailable: 1` — 노드 점검 시에도 최소 1 Pod 보장.
- **HPA**: backend CPU 70% / 메모리 80%, 2–10 Pod. frontend CPU 70%, 2–5 Pod. metrics-server 필요.
- **클러스터 오토스케일러**: EKS 노드 그룹 min 2 / max 5 자동 조정.
- **데이터 백업**: RDS 자동 백업 7일 + 삭제 시 최종 스냅샷(`final_snapshot_identifier_prefix`).

## 8. 부트스트랩·재해 복구 요지

1. Terraform `apply -target=module.eks` → 클러스터 생성.
2. Terraform `apply` → 애드온 helm_releases(ESO/LBC/external-dns/...) → Argo CD → argocd_bootstrap.
3. Secrets Manager에 환경별·Argo 시크릿 값 주입(수동, [Task 11 운영 런북](superpowers/plans/2026-05-28-argocd-gitops.md) 참조).
4. 클러스터 내부에서 1회 SQL: `CREATE DATABASE student_mgmt_{dev,staging,prod}`.
5. Argo가 git을 watch하여 자동 동기화 시작.

복구: Terraform 상태(`s3://student-mgmt-tfstate`) + git(매니페스트) + ECR(이미지 immutable) + RDS 자동 백업의 조합으로 클러스터 전체를 재구축 가능. ECR `image_tag_mutability: IMMUTABLE`이라 과거 배포 SHA 재배포가 결정론적.
