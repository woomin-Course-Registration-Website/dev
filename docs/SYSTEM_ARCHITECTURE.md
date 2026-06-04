# 시스템 아키텍처

> 본 문서는 **배포된 시스템의 런타임 토폴로지**를 다룬다. 백엔드 코드 내부 구조(레이어/패키지/인증 처리)는 [ARCHITECTURE.md](ARCHITECTURE.md)를, AWS 리소스 카탈로그·IAM·Terraform 구성은 [INFRASTRUCTURE.md](INFRASTRUCTURE.md)를 참조한다.

## 1. 한 화면 요약

```
                          사용자 (교사 / 학생 / 학부모)
                                    │
                ┌───────────────────┴─────────────────────┐
                │ SPA 자산 요청                            │ REST API 호출
                │ https://<branch>.<app-id>                │ https://<api-id>.execute-api.…
                │   .amplifyapp.com                        │   .amazonaws.com/api/…
                ▼                                          ▼
        ┌────────────────────┐                  ┌──────────────────────────┐
        │   AWS Amplify       │                  │  API Gateway HTTP API     │
        │  React 빌드 + CDN   │                  │  (환경별 3개 분리)         │
        │  ACM TLS, branch별  │                  │  • TLS 종료              │
        │  매핑                │                  │  • throttling            │
        │  develop→dev.       │                  │  • CORS                  │
        │  staging→staging.   │                  │  • 단순 진입점 (인증 X)   │
        │ main→main.<app-id>  │                  └────────┬─────────────────┘
        └────────────────────┘                            │ VPC Link
                                                          │ (private subnet ENI)
                                                          ▼
                                          ┌──────────────────────────────┐
                                          │  private ALB (scheme:internal) │
                                          │  환경별 1개, group.name으로    │
                                          │  aws-load-balancer-controller가│
                                          │  Ingress에서 자동 생성        │
                                          └────────┬─────────────────────┘
                                                   │ HTTP
                                          ┌────────┴─────────────────────┐
                                          │  EKS 1.30 클러스터            │
                                          │   (백엔드 전용 워크로드)       │
                                          │                              │
                                          │  student-mgmt-{dev,staging,  │
                                          │   prod} 네임스페이스 각각:    │
                                          │   • backend Deployment       │
                                          │   • Service, HPA, PDB        │
                                          │   • Ingress(internal scheme) │
                                          │   • Secret(SealedSecret에서  │
                                          │     materialize)             │
                                          │   • ServiceMonitor           │
                                          │   • NetworkPolicy            │
                                          │                              │
                                          │  argocd ns:                  │
                                          │   • Argo CD + ApplicationSet │
                                          │                              │
                                          │  sealed-secrets ns:          │
                                          │   • sealed-secrets-controller│
                                          │                              │
                                          │  monitoring ns:              │
                                          │   • kube-prometheus-stack    │
                                          │     (Prometheus + Grafana +  │
                                          │      Alertmanager + CRDs)    │
                                          └────────┬─────────────────────┘
                                                   │ JDBC
                                                   ▼
                                          ┌──────────────────────────────┐
                                          │  RDS MySQL 8 (Multi-AZ)       │
                                          │  단일 인스턴스 + 환경별        │
                                          │  논리 DB 3개                  │
                                          │  (student_mgmt_{dev,staging,  │
                                          │   prod})                     │
                                          └──────────────────────────────┘
```

**핵심 원칙:**
- 프론트엔드(Amplify)와 API(API Gateway)는 **서로 다른 호스트**로 분리. CORS는 Spring Boot 백엔드와 API Gateway가 처리.
- ALB는 **internet-facing 아님**. 인터넷에서 직접 도달 불가, API Gateway VPC Link만 진입 가능.
- 시크릿은 git에 `SealedSecret`(암호화) 형태로 커밋, 클러스터 내 controller가 복호화하여 Secret materialize.
- 인증·인가는 Spring Security가 모두 담당. API Gateway는 단순 진입점.

## 2. 환경 분리 매트릭스

| 환경 | EKS 네임스페이스 | Amplify 브랜치 → SPA URL | API URL | ALB group.name | RDS 스키마 | Argo Application |
|------|------------------|--------------------------|---------|----------------|------------|-------------------|
| dev | `student-mgmt-dev` | `develop` → `https://develop.<app-id>.amplifyapp.com` | `https://<api-id>.execute-api.ap-northeast-2.amazonaws.com/api` | `student-mgmt-dev` | `student_mgmt_dev` | `student-mgmt-dev` (auto) |
| staging | `student-mgmt-staging` | `staging` → `https://staging.<app-id>.amplifyapp.com` | `https://<api-id>.execute-api…` | `student-mgmt-staging` | `student_mgmt_staging` | `student-mgmt-staging` (auto, PR gate) |
| prod | `student-mgmt-prod` | `main` → `https://main.<app-id>.amplifyapp.com` | `https://<api-id>.execute-api…` | `student-mgmt-prod` | `student_mgmt_prod` | `student-mgmt-prod` (auto, PR gate) |

> 실제 URL 확인: `terraform output amplify_branch_urls` / `terraform output apigateway_endpoints`. 커스텀 도메인 없이 AWS 기본 URL 사용.

**공유 자원:** EKS 클러스터·노드 그룹·RDS 인스턴스·Amplify 앱(단일, 브랜치만 분기)·애드온.
**분리 자원:** 네임스페이스·ALB·API Gateway HTTP API·Route53 레코드·RDS 논리 DB·Argo Application·Amplify 브랜치.

## 3. 컴포넌트별 책임

### 3.1 프론트엔드 — AWS Amplify
- Amplify가 GitHub repo의 `frontend/` 디렉토리를 watch.
- 브랜치 webhook으로 자동 빌드 → 자동 배포.
- `amplify.yml`(repo 루트)에서 `npm install && npm run build` → `dist/` 산출물 CDN 호스팅.
- 환경변수 `VITE_API_BASE_URL`이 브랜치별로 다르게 주입 — 값은 해당 환경 API Gateway의 기본 invoke URL + `/api` (Terraform이 `aws_apigatewayv2_api.env[env].api_endpoint`를 통해 자동 설정).
- TLS·인증서·CDN 모두 Amplify가 관리. EKS 부담 없음.

### 3.2 API 진입점 — API Gateway HTTP API
- **환경마다 별도 API 3개**. 각자 AWS 기본 invoke URL(`https://<api-id>.execute-api.ap-northeast-2.amazonaws.com`), 자신의 VPC Link.
- HTTP API는 REST API보다 저비용·단순. JWT authorizer를 쓰지 않으므로 HTTP API로 충분.
- 모든 경로(`ANY /{proxy+}`)를 VPC Link 통해 환경 ALB로 패스스루.
- CORS: `allow_origins`에 해당 환경 Amplify 기본 URL만 허용(`https://<branch>.<app-id>.amplifyapp.com`).
- 인증서: AWS 기본 도메인은 AWS가 관리(별도 ACM 인증서 불필요).

### 3.3 내부 라우팅 — private ALB
- 환경별로 `aws-load-balancer-controller`가 `Ingress`를 보고 ALB를 자동 생성.
- `scheme: internal` — 인터넷에서 직접 접근 불가, VPC Link만 진입 가능.
- listen-ports는 HTTP:80만 (TLS는 API Gateway에서 이미 종료됨).
- 모든 경로(`/`)를 backend Service로 라우팅(과거의 `/swagger-ui`, `/v3/api-docs` 분기는 backend가 그대로 처리하므로 불필요).
- `alb.ingress.kubernetes.io/group.name`을 환경별로 다르게 지정해 ALB 분리.

### 3.4 백엔드 — Spring Boot on EKS
- Spring Boot 3.3 / Java 21 / Spring Security JWT (Access 15분 + Refresh 7일).
- 시크릿(`backend-credentials`)은 Pod의 `envFrom: secretRef`로 주입 — `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
- 환경별 `student-mgmt-{env}` 네임스페이스에 동일한 Kustomize 베이스 + 환경별 패치로 배포.
- 자체 메트릭은 `/actuator/prometheus` 엔드포인트로 노출, `ServiceMonitor`가 30초 간격 스크랩.

### 3.5 데이터 — RDS MySQL (Multi-AZ)
- `db.t3.medium`, MySQL 8.0, Multi-AZ 스탠바이.
- 단일 인스턴스에 환경별 논리 DB 3개(`student_mgmt_dev`/`_staging`/`_prod`).
- 환경별 자격증명은 SealedSecret으로 분리(같은 RDS, 다른 사용자/DB 가능).
- 자동 백업 7일, deletion protection 활성, `final_snapshot_identifier_prefix` 설정.

### 3.6 시크릿 — Sealed Secrets
- Bitnami `sealed-secrets-controller`가 `sealed-secrets` 네임스페이스에 배포(클러스터 1개).
- 운영자가 로컬에서 `kubeseal`로 시크릿 암호화 → 암호화된 `SealedSecret` YAML을 git에 커밋.
- Argo가 sync → controller가 자기 비대칭 키로 복호화 → 일반 `Secret` 리소스 materialize.
- 컨트롤러 키는 클러스터 부트스트랩 시 자동 생성, 운영자가 별도 백업 보관 필수(클러스터 재생성 시 복원용).

### 3.7 관측 — kube-prometheus-stack
- `monitoring` 네임스페이스에 Prometheus + Grafana + Alertmanager + 모든 CRDs(`ServiceMonitor` 등) 설치.
- backend `ServiceMonitor`가 자동 스크랩.
- Grafana/Prometheus UI는 **port-forward 전용**(`kubectl port-forward svc/...`) — 인터넷 노출 없음.
- 로그는 별도 수집 인프라 없음 — `kubectl logs` 또는 `k9s`로 조회. Pod 재시작 시 이전 로그 손실.
- AWS CloudWatch 알람/대시보드 미사용.

### 3.8 GitOps — Argo CD
- `argocd` 네임스페이스에 Argo CD 설치(Terraform `helm_release "argocd"`).
- `ApplicationSet`(list 제너레이터)이 `student-mgmt-{dev,staging,prod}` Application 3개를 자동 생성.
- 각 Application이 `k8s/overlays/{env}` 경로를 watch하여 자기 네임스페이스에 sync.
- 모든 환경 자동 sync(prod도 자동, 단 승격 PR 머지가 사람 게이트).
- Argo UI는 **port-forward 전용**(`kubectl port-forward svc/argocd-server -n argocd 8080:443`).

## 4. 요청 흐름

### 4.1 SPA 정적 자산 로드
```
브라우저 → https://<host> (Amplify CDN)
         → React 번들·이미지·CSS 제공 (TLS는 Amplify 관리)
```

### 4.2 REST API 호출
```
브라우저 → https://<api-id>.execute-api.ap-northeast-2.amazonaws.com/api/... (CORS preflight + 실제 요청)
         ↓ TLS 종료, throttling, CORS
       API Gateway HTTP API
         ↓ VPC Link (private subnet ENI)
       private ALB (HTTP)
         ↓ target-type: ip
       backend Pod (:8080)
         ↓ Spring Security JWT 검증, @PreAuthorize RBAC
         ↓ JDBC
       RDS MySQL (env별 논리 DB)
         ↓ 응답
       backend → ALB → API Gateway → 브라우저
```

## 5. 배포 흐름 (GitOps + Amplify)

```
┌── 프론트엔드 ────────────────────────────────────┐
│ 개발자가 frontend/* 변경 → git push (develop 등)  │
│   ↓ GitHub webhook                              │
│ Amplify Console: 자동 빌드 → 자동 배포           │
│ branch=develop → develop.<app-id>.amplifyapp.com│
│ branch=staging → staging.<app-id>.amplifyapp.com│
│ branch=main    → main.<app-id>.amplifyapp.com   │
│ GitHub Actions 워크플로우 불필요.                 │
└─────────────────────────────────────────────────┘

┌── 백엔드 ────────────────────────────────────────┐
│ 개발자가 src/* 변경 → git push main              │
│ (paths-ignore: k8s/**, docs/**, frontend/**)    │
│   ↓                                             │
│ GitHub Actions cd.yml:                          │
│   1. AWS OIDC (ECR PowerUser만, 클러스터 권한 X) │
│   2. backend 이미지 build → ECR (불변 태그       │
│      :${github.sha})                            │
│   3. kustomize edit set image                   │
│      → k8s/overlays/dev/kustomization.yaml      │
│   4. GITHUB_TOKEN으로 main에 write-back 커밋·push │
│   ↓                                             │
│ Argo CD가 git watch → student-mgmt-dev Application│
│ 자동 sync → 새 이미지 롤아웃                      │
└─────────────────────────────────────────────────┘

┌── 환경 승격 (staging/prod) ──────────────────────┐
│ GitHub Actions promote.yml(workflow_dispatch):  │
│   from=dev, to=staging → 대상 overlay의         │
│   kustomization image 태그를 원본에서 복사 →    │
│   PR 자동 생성. 머지 = 배포 게이트.               │
│   ↓                                             │
│ Argo가 staging/prod Application sync.           │
└─────────────────────────────────────────────────┘
```

**Sync wave**: Argo 어노테이션으로 Ingress(`sync-wave: "2"`)는 Deployment 이후 reconcile.

**CI 권한 모델**: GitHub Actions 역할은 ECR PowerUser만 보유, 클러스터 자격증명 없음. 모든 apply 권한은 Argo CD(클러스터 내부 RBAC).

## 6. 보안 경계

| 계층 | 메커니즘 |
|------|---------|
| 외부 → Amplify | TLS (ACM, Amplify 관리), Amplify 호스트 도메인 검증 |
| 외부 → API Gateway | TLS 1.2+, throttling, CORS allow_origins 화이트리스트 |
| API Gateway ↔ ALB | VPC Link (private subnet ENI 경유, VPC 내부 통신) |
| ALB ↔ Pod | HTTP, ALB SG가 VPC Link SG에서만 허용 + 노드 SG가 ALB SG에서만 허용 |
| Pod ↔ Pod | NetworkPolicy: backend Pod는 VPC CIDR 10.0.0.0/16의 8080만 허용 |
| Pod ↔ RDS | RDS SG가 EKS 노드 SG의 3306만 허용 |
| 시크릿 git 저장 | SealedSecret(클러스터 비대칭 키로 암호화) |
| API 인증·인가 | Spring Security JWT + `@PreAuthorize` 메서드 단위 RBAC |
| CI → AWS | GitHub OIDC, ECR PowerUser 한 정책만 |
| CI → 클러스터 | 없음 (Argo가 유일 apply 주체) |
| 클러스터 → AWS | IRSA (애드온별 최소 권한, OIDC trust) |
| Argo UI / Grafana | 공용 노출 없음, port-forward 전용 |

## 7. 가용성·확장

- **Multi-AZ**: VPC 2 AZ, EKS 노드 그룹·NAT(`one_nat_gateway_per_az`)·RDS Multi-AZ 스탠바이.
- **무중단 배포**: RollingUpdate(`maxSurge: 1, maxUnavailable: 0`), readiness/liveness probe, `terminationGracePeriodSeconds: 60`(Spring graceful shutdown).
- **PDB**: backend `minAvailable: 1`.
- **HPA**: backend CPU 70% / memory 80%, 2–10 Pod. metrics-server 필요.
- **클러스터 오토스케일러**: 노드 그룹 min 2 / max 5.
- **데이터 보호**: RDS 자동 백업 7일 + 삭제 시 final snapshot.

## 8. 부트스트랩·복구 요지

**2-phase apply** 필요(API Gateway가 ALB에 의존, ALB는 Argo가 sync해야 존재):

1. `terraform apply -target=module.eks` — EKS 클러스터 생성.
2. `terraform apply` (단, API Gateway 리소스는 -target 또는 -exclude로 제외) — 애드온·Sealed Secrets·Argo·Amplify까지 설치. 이때 Argo가 Ingress sync로 환경별 ALB 생성.
3. 운영자가 `kubeseal`로 환경 시크릿 암호화 → git 커밋 → Argo sync → backend Pod 기동.
4. RDS에 SQL로 `CREATE DATABASE student_mgmt_{dev,staging,prod}` (1회).
5. `terraform apply` (전체) — 이번엔 ALB가 존재하므로 API Gateway 리소스 resolve 가능.

자세한 절차는 [Task 21 컷오버 런북](superpowers/plans/2026-06-03-system-redesign.md#task-21) 참조.

**복구**: Terraform state(S3) + git(매니페스트 + 암호화된 SealedSecret) + ECR 불변 이미지 + RDS 자동 백업 + Sealed Secrets 키 백업의 조합으로 전체 재구축 가능. SealedSecret 키를 잃으면 모든 시크릿을 다시 만들어야 함.
