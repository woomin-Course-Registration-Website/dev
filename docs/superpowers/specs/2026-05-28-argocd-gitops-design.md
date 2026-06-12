# Argo CD 기반 GitOps CD 전환 설계

> 작성일: 2026-05-28 · 대상: 교사용 학생 성적/상담 관리 시스템 (EKS)
> 상태: 설계 승인됨, 구현 계획 작성 전

## 1. 목표

push 방식 CD(`cd.yml`이 OIDC로 AWS 인증 후 EKS에 직접 `kubectl apply`)를 **Argo CD pull 방식 GitOps**로 전환한다. 핵심 효과:

- **CI에서 클러스터 자격증명 제거** — GitHub Actions 역할의 EKS 클러스터-admin 권한 회수. Argo(클러스터 내부)가 유일한 apply 주체가 된다.
- **선언적·감사 가능한 배포** — 모든 배포가 git 커밋. `git revert`로 롤백.
- **다중 환경(dev/staging/prod)** 을 단일 클러스터 + 네임스페이스 분리로 운영.

## 2. 현재 상태 (As-Is)

- **모노레포** `dev`: 앱 코드 + `k8s/`(plain YAML) + `infra/terraform/` + `.github/workflows/`.
- **CD** (`.github/workflows/cd.yml`): `build-and-push`(ECR push) → `deploy`(동일 GitHub Actions 역할로 `aws eks update-kubeconfig` 후 `envsubst | kubectl apply` + `kubectl rollout status`).
- **GitHub Actions IAM 역할**: `AmazonEKSClusterAdminPolicy` access entry(`eks.tf:52-62`) + ECR PowerUser + `eks:DescribeCluster`(`iam.tf:44-56`). → 클러스터 admin 보유가 가장 큰 보안 리스크.
- **Terraform 애드온**: aws-load-balancer-controller, external-secrets(ESO), cluster-autoscaler, metrics-server, external-dns 를 `helm_release`로 관리. `helm`/`kubernetes` 프로바이더는 `aws eks get-token` exec로 이미 구성됨(`main.tf:59-79`).
- **envsubst 플레이스홀더 4종**: `${ECR_BACKEND_IMAGE}:${IMAGE_TAG}`, `${ECR_FRONTEND_IMAGE}:${IMAGE_TAG}`(이미지), `${ACM_CERT_ARN}`(TF output), `${ALB_HOSTNAME}`(repo variable).
- **ACM 인증서**: 와일드카드(`*.${domain_name}`, `acm.tf:9`) — 모든 서브도메인 커버.
- **ESO**: `ClusterSecretStore aws-secrets-manager`(클러스터 스코프) + `student-mgmt` 네임스페이스의 `ExternalSecret backend-secrets`가 Secrets Manager(`student-mgmt/db`, `student-mgmt/jwt`)에서 DB/JWT 동기화. ESO IRSA 정책은 `student-mgmt/*` 허용.
- **이미지 태그**: `docker/metadata-action`이 branch ref, semver(`v*.*.*`), `sha-` prefix 태그 생성.

## 3. 확정 결정사항

| # | 항목 | 선택 | 근거 |
|---|------|------|------|
| 1 | 매니페스트 위치 | 모노레포(`dev`), Argo가 `k8s/overlays/*` watch | 단일 레포 유지, paths-ignore로 CI 루프 차단 |
| 2 | 환경 범위 | dev / staging / prod | |
| 3 | 클러스터 구조 | 단일 EKS + 네임스페이스 분리(`student-mgmt-{env}`) | 비용 최소·단순. 애드온 공용 |
| 4 | 매니페스트 구조 | Kustomize base + overlays | plain YAML에서 점진 전환, 이미지/네임스페이스 트랜스포머 활용 |
| 5 | 이미지 전파 | CI write-back(`kustomize edit set image`) | git 감사 이력, Image Updater 컴포넌트 불필요 |
| 6 | 승격 흐름 | dev 자동 / staging·prod PR 승격 | PR 머지가 사람 게이트 |
| 7 | Argo 설치 | Terraform `helm_release "argocd"` + 로컬 bootstrap 차트 | 기존 애드온 패턴 일치, chicken-egg 회피 |
| 8 | App 생성 | ApplicationSet(list 제너레이터) | DRY, 환경 추가는 list 항목 1개 |
| 9 | UI 인증 | 부트스트랩 admin, 비밀번호 Secrets Manager→ESO 동기화 | 기존 시크릿 패턴 일치, SSO 추후 추가 가능 |

확정된 open item(추천안 채택):
- **prod sync 정책**: automated(prune+selfHeal). 승격 PR이 게이트. 추후 manual sync로 전환 가능.
- **호스트명**: `dev.<domain>` / `staging.<domain>` / `app.<domain>`(prod). `<domain>` = `var.domain_name`.
- **데이터 레이어**: RDS 인스턴스 1개 + 논리 데이터베이스 3개, Secrets Manager에 환경별 자격증명.

## 4. 목표 아키텍처 (To-Be)

```
개발자 → git push (앱 코드)
   │
   ▼
GitHub Actions [CI]            AWS OIDC 역할 = ECR 전용 (클러스터 권한 없음)
   ├─ build & push → ECR (불변 태그 sha-<gitsha>)
   └─ write-back → kustomize edit set image (overlays/dev) → main 커밋 (GITHUB_TOKEN)
   │
   ▼ (git watch, pull)
Argo CD (EKS 내부, argocd 네임스페이스)
   ├─ ApplicationSet → student-mgmt-{dev,staging,prod}
   └─ 각 Application = k8s/overlays/{env} → 네임스페이스 student-mgmt-{env}
   │
   ▼ (sync, 유일한 apply 주체)
EKS 워크로드 (backend/frontend/ingress/externalsecret …)
```

승격: `workflow_dispatch`(또는 수동 PR)가 dev overlay의 핀된 태그를 staging→prod overlay로 복사하는 PR 생성 → 머지 → Argo가 해당 환경 sync.

## 5. 상세 설계

### 5.1 레포 구조 (Kustomize)

```
k8s/
  base/
    kustomization.yaml
    namespace.yaml              # "student-mgmt"; namespace 트랜스포머가 환경별 rename
    backend/{deployment,service,hpa,pdb}.yaml
    frontend/{deployment,service,hpa}.yaml
    ingress.yaml                # 플레이스홀더 제거, overlay 패치로 값 주입
    network-policy.yaml
    external-secrets/
      backend-secrets.yaml      # ExternalSecret (네임스페이스 자원)
  overlays/
    dev/      kustomization.yaml + patches
    staging/  kustomization.yaml + patches
    prod/     kustomization.yaml + patches
  bootstrap/                    # Terraform이 렌더링 (5.3)
    Chart.yaml, templates/
      appproject.yaml
      applicationset.yaml
      clustersecretstore.yaml   # 클러스터 스코프 싱글톤 — 여기서만 1회 정의
      argocd-admin-externalsecret.yaml
```

플레이스홀더 제거:
- **이미지**: base는 bare name(`student-mgmt/backend`, `student-mgmt/frontend`). overlay `images:` 필드가 실제 ECR 태그를 핀. CI가 `kustomize edit set image`로 갱신.
- **`${ACM_CERT_ARN}`**: 단일 와일드카드 인증서 → 각 overlay의 Ingress 어노테이션 정적 패치값(또는 base 공통값).
- **`${ALB_HOSTNAME}`**: 환경별 Ingress 패치(`dev/staging/app.<domain>`), `alb.ingress.kubernetes.io/group.name`을 환경별로 달리해 ALB 분리.
- **네임스페이스**: overlay `namespace: student-mgmt-{env}` → Kustomize 트랜스포머가 모든 네임스페이스 자원 및 `Namespace` 오브젝트 자체를 rename.
- **ExternalSecret 경로**: overlay 패치로 `remoteRef.key`를 `student-mgmt/{env}/db`, `student-mgmt/{env}/jwt`로 변경.

**ClusterSecretStore 주의**: 클러스터 스코프 싱글톤이므로 per-env 앱에서 제외하고 bootstrap 차트에서 1회만 정의. per-env `ExternalSecret`은 공유 스토어를 참조.

### 5.2 이미지 전파 & 승격 (CI write-back)

`main` push 시:
1. **빌드 잡**: backend+frontend 빌드 → ECR push, **불변 태그** `sha-<gitsha>`(릴리스 태그 시 semver 병행). AWS OIDC 역할은 ECR 전용.
2. **write-back 잡**: `overlays/dev`에 `kustomize edit set image student-mgmt/backend=<ecr>/student-mgmt/backend:sha-<gitsha>`(frontend 동일) → 빌트인 `GITHUB_TOKEN`(`contents: write`)으로 `main` 커밋.
3. Argo dev Application 자동 sync.

**승격**: `workflow_dispatch`가 dev overlay의 현재 핀 태그를 staging→prod overlay에 반영하는 PR 생성. 머지가 게이트.

**루프 방지(모노레포)** — 2중 가드:
- `GITHUB_TOKEN` author 커밋은 새 워크플로우를 트리거하지 않음(GitHub 기본 동작).
- 빌드 워크플로우에 `paths-ignore: ['k8s/**']`.

### 5.3 Argo 설치 & 부트스트랩 (Terraform)

`infra/terraform/eks.tf`에 기존 애드온 패턴으로 추가:

```hcl
resource "helm_release" "argocd" {            # argo-cd 차트, ns "argocd", create_namespace=true
  depends_on = [module.eks]
  # values:
  #   configs.params."server.insecure" = true        # ALB가 TLS 종료
  #   server.ingress (5.4)
  #   configs.cm."resource.customizations.health.external-secrets.io_ExternalSecret" (5.5)
}

resource "helm_release" "argocd_bootstrap" {  # k8s/bootstrap/ 로컬 차트
  depends_on = [helm_release.argocd, helm_release.external_secrets]
  # 렌더링: AppProject, ApplicationSet, ClusterSecretStore, argocd-admin ExternalSecret
}
```

**`kubernetes_manifest` 대신 로컬 helm 차트 사용 이유**: `kubernetes_manifest`는 plan 시점 CRD 검증 → 새 클러스터에서 Application/ApplicationSet CRD 부재로 실패(chicken-egg). `helm_release`는 클라이언트 사이드 렌더링이라 plan 시점 CRD 검증 없음.

**부트스트랩 순서**:
1. `terraform apply -target=module.eks` (기존 문서화된 1단계)
2. `terraform apply` → 애드온(lbc, eso, external-dns 등) → `argocd` → `argocd_bootstrap`

**자기 관리 제외**: Argo 설치 자체는 Terraform 소유 유지(Argo가 자신을 prune 하는 사고 방지). Argo는 앱 워크로드만 소유.

### 5.4 Argo UI 노출 & 인증

- **노출**: argo-cd 차트의 server.ingress 사용 → 호스트 `argocd.<domain>`, ALB internet-facing, 80/443 + ssl-redirect, `certificate-arn`=기존 와일드카드 ACM, `external-dns.alpha.kubernetes.io/hostname` 어노테이션으로 Route53 레코드 자동 생성. `server.insecure=true`로 ALB가 TLS 종료, argocd-server와는 HTTP(이중 TLS 방지).
- **인증**: 빌트인 `admin`. 차트 랜덤 초기 시크릿 대신 `argocd` 네임스페이스 `ExternalSecret`이 Secrets Manager(`student-mgmt/argocd/admin`)에서 bcrypt 비밀번호를 `argocd-secret`(`admin.password`, `admin.passwordMtime`)으로 동기화. 기본 RBAC. GitHub SSO/Dex는 추후 재작업 없이 추가 가능.

### 5.5 ApplicationSet, sync wave, health

**ApplicationSet** (list 제너레이터), 환경별 Application 템플릿:
- `source`: dev 레포, `path: k8s/overlays/{{env}}`
- `destination`: in-cluster, 네임스페이스 `student-mgmt-{{env}}`
- `project`: `student-mgmt` (소스 레포 + 허용 대상 네임스페이스 3개로 제한하는 AppProject)
- `syncPolicy`: dev/staging/prod 모두 `automated {prune:true, selfHeal:true}` (prod 게이트=승격 PR; manual 전환 옵션 명시)
- `syncOptions`: `CreateNamespace=true`, `ServerSideApply=true`

**Sync wave** (base 어노테이션 `argocd.argoproj.io/sync-wave`):
- wave 0 — `ExternalSecret`
- wave 1 — Deployment, Service, HPA, PDB, NetworkPolicy
- wave 2 — Ingress (Service 존재 후, ALB 타깃 그룹 resolve 위해)

**Health 게이팅**: Argo values에 `external-secrets.io/ExternalSecret`용 `resource.customizations` health check(Lua, `Ready` 조건) 추가 → wave 0은 Secret materialize 후 완료 → wave 1 backend Deployment가 시작.

### 5.6 CI 역할 축소 & Terraform IAM 정리

- **`cd.yml`**: `deploy` 잡 전체 삭제(kubectl / envsubst / `aws eks update-kubeconfig` / rollout / gettext 설치). write-back 잡 추가(5.2). `permissions: contents: write` 추가.
- **`eks.tf`**: `access_entries.github_actions` 클러스터 admin 권한 제거(52-62줄).
- **`iam.tf`**: `aws_iam_role_policy "github_actions_eks"`(`eks:DescribeCluster`) 제거. ECR PowerUser 유지.
- **결과**: GitHub Actions 역할=이미지 push만, `GITHUB_TOKEN`=매니페스트 커밋만. 둘 다 클러스터 접근 불가.

## 6. 파일별 변경 요약

**생성**:
- `k8s/base/kustomization.yaml`, `k8s/base/**`(기존 매니페스트 이동·정리)
- `k8s/overlays/{dev,staging,prod}/kustomization.yaml` + 패치 파일
- `k8s/bootstrap/`(로컬 helm 차트): `Chart.yaml`, `templates/{appproject,applicationset,clustersecretstore,argocd-admin-externalsecret}.yaml`
- `.github/workflows/`에 승격(promotion) 워크플로우(`workflow_dispatch`)

**수정**:
- 기존 `k8s/*.yaml` → base/overlays로 재배치, 플레이스홀더 제거, sync-wave 어노테이션 추가
- `.github/workflows/cd.yml` → `deploy` 잡 제거, write-back 잡 추가, `paths-ignore` 적용
- `infra/terraform/eks.tf` → `helm_release "argocd"`, `helm_release "argocd_bootstrap"` 추가, github_actions access entry 제거
- `infra/terraform/iam.tf` → `github_actions_eks` 정책 제거
- `infra/terraform/secrets.tf` → 환경별 Secrets Manager 항목(`student-mgmt/{env}/db`, `/jwt`, `student-mgmt/argocd/admin`) 추가
- `infra/terraform/rds.tf` → 논리 DB 3개(또는 환경별 초기화) — 데이터 레이어 확정안 반영
- `infra/terraform/outputs.tf` → argocd URL 등 output 추가(선택)

**삭제 대상 검토**: `appspec.yml`, `docker-compose.prod.yml`, `scripts/{ec2-setup,start,stop,validate}.sh`는 이미 git status에서 삭제 staged 상태(CodeDeploy 기반 배포 잔재) — GitOps 전환과 정합. 함께 정리.

## 7. 마이그레이션 절차

1. Kustomize 구조로 매니페스트 리팩터 + `kustomize build overlays/<env>`로 로컬 렌더 검증.
2. Terraform: `helm_release "argocd"` + bootstrap 차트 추가 → `terraform apply`로 Argo 설치 및 ApplicationSet 부트스트랩.
3. Argo UI에서 각 환경 Application sync 상태/health 확인(수동 sync로 1차 검증).
4. CI 전환: `cd.yml`에서 deploy 잡 제거 + write-back 잡 활성화.
5. IAM 축소: github_actions access entry / `eks:DescribeCluster` 제거 후 `terraform apply`.
6. 한 라운드 end-to-end 검증(코드 push → ECR → write-back 커밋 → Argo sync → 롤아웃) 후 자동 sync 활성화.

## 8. 보안 영향

- GitHub Actions 역할에서 클러스터 admin 권한 완전 제거 → 공급망/CI 침해 시 클러스터 직접 조작 불가.
- apply 권한은 Argo(클러스터 내부 RBAC)로 일원화.
- 시크릿은 계속 Secrets Manager + ESO(IRSA)로만 주입. Argo admin 비밀번호도 동일 경로.

## 9. 가정 및 범위 외

**가정**:
- 단일 EKS 클러스터, 3개 네임스페이스로 환경 분리.
- RDS 1개 + 논리 DB 3개(비용 평탄화). 별도 RDS 분리는 추후 결정 가능.
- prod 자동 sync(승격 PR이 게이트).

**범위 외(추후)**:
- GitHub SSO/Dex 인증.
- Argo Rollouts(카나리/블루그린) — 현재 RollingUpdate 유지.
- 애드온(lbc/eso 등)의 GitOps 이관 — 당분간 Terraform 소유 유지.
- 환경별 클러스터 분리(hub-spoke).

## 10. 검증 방법

- `kustomize build k8s/overlays/<env>` 정상 렌더 + 플레이스홀더 잔존 없음.
- Argo Application 3개 Synced/Healthy, sync wave 순서대로 진행(ExternalSecret→워크로드→Ingress).
- 코드 push 시 ECR 태그 생성 + dev overlay write-back 커밋 + dev 자동 sync 확인.
- 승격 PR 머지 시 staging/prod 반영 확인.
- IAM 축소 후 CI에서 클러스터 접근 시도가 불가함을 확인(deploy 잡 부재).
