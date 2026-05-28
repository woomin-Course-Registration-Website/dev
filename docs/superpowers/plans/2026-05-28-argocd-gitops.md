# Argo CD GitOps CD 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** push 방식 CD(GitHub Actions가 EKS에 직접 `kubectl apply`)를 Argo CD pull 방식 GitOps로 전환하고, CI에서 클러스터 자격증명을 완전히 제거한다.

**Architecture:** `k8s/`를 Kustomize base+overlays(dev/staging/prod)로 재편하고, Terraform `helm_release`로 Argo CD와 로컬 bootstrap 차트(AppProject/ApplicationSet/ClusterSecretStore/ESO 시크릿)를 설치한다. CI는 ECR push + dev overlay 이미지 태그 write-back 커밋만 수행하고, Argo가 git을 watch하여 유일한 apply 주체가 된다.

**Tech Stack:** Kustomize, Argo CD(argo-cd helm chart), Terraform(helm/kubernetes provider), External Secrets Operator, AWS EKS/ECR/Secrets Manager, GitHub Actions.

---

## 사전 준비 / 전제

- 이 작업은 `develop`에서 `feature/EP-XX-argocd-gitops` 브랜치를 만들어 진행한다(저장소 Git 컨벤션).
- 검증 도구 필요: `kubectl`(내장 kustomize 사용), `helm`, `terraform`. 미설치 시 해당 검증 단계는 로컬에서 설치 후 수행.
- **Terraform `plan`/`apply`와 Argo 실제 sync는 라이브 AWS 자격증명/클러스터가 필요**하다. 본 계획의 자동 검증은 렌더/validate 수준까지이며, 실제 적용·동기화 검증은 Task 11(수동 컷오버)에서 수행한다.
- 다음 값은 환경 고유이므로 실제 값으로 치환해야 한다(획득 출처 명시):
  - `<ACCOUNT_ID>` = `aws sts get-caller-identity --query Account --output text`
  - ECR URL = `terraform output -raw ecr_backend_url` / `ecr_frontend_url`
  - 와일드카드 ACM ARN = `terraform output -raw acm_cert_arn`
  - `<domain>` = `var.domain_name` (terraform.tfvars)
  - git repoURL = `https://github.com/<github_org>/<github_repo>.git`

---

## File Structure

**생성**
- `k8s/base/kustomization.yaml` — base 리소스 목록
- `k8s/base/{namespace,ingress,network-policy}.yaml`, `k8s/base/backend/*`, `k8s/base/frontend/*`, `k8s/base/external-secrets/backend-secrets.yaml` — base 매니페스트(대부분 이동)
- `k8s/overlays/{dev,staging,prod}/kustomization.yaml` + `ingress-patch.yaml` + `externalsecret-patch.yaml`
- `k8s/bootstrap/` 로컬 helm 차트: `Chart.yaml`, `values.yaml`, `templates/{appproject,applicationset,clustersecretstore,argocd-admin-externalsecret,argocd-repo-externalsecret}.yaml`
- `.github/workflows/promote.yml` — 환경 승격 워크플로우

**수정**
- `.github/workflows/cd.yml` — deploy 잡 제거, write-back 잡 추가, `paths-ignore`
- `infra/terraform/eks.tf` — `helm_release "argocd"`, `helm_release "argocd_bootstrap"` 추가, `access_entries.github_actions` 제거
- `infra/terraform/iam.tf` — `github_actions_eks` 정책 제거
- `infra/terraform/secrets.tf` — 환경별 Secrets Manager 항목 추가
- `infra/terraform/variables.tf` / `outputs.tf` — argocd 관련 변수/출력
- `CLAUDE.md` — CI/CD 섹션 갱신

**삭제**
- `appspec.yml`, `docker-compose.prod.yml`, `scripts/{ec2-setup,start,stop,validate}.sh` (CodeDeploy 잔재; 이미 삭제 staged)

---

## Task 1: Kustomize base 구성

**Files:**
- Create: `k8s/base/kustomization.yaml`
- Move (내용 무변경): `k8s/namespace.yaml`→`k8s/base/namespace.yaml`, `k8s/network-policy.yaml`→`k8s/base/network-policy.yaml`, `k8s/backend/{service,hpa,pdb}.yaml`→`k8s/base/backend/`, `k8s/frontend/{service,hpa}.yaml`→`k8s/base/frontend/`
- Modify(이동+편집): `k8s/backend/deployment.yaml`, `k8s/frontend/deployment.yaml`, `k8s/ingress.yaml`, `k8s/external-secrets/backend-secrets.yaml`
- Delete: `k8s/external-secrets/secret-store.yaml` (ClusterSecretStore → bootstrap 차트로 이동, Task 3)

- [ ] **Step 1: 무변경 파일 이동**

```bash
cd <repo>
mkdir -p k8s/base/backend k8s/base/frontend k8s/base/external-secrets
git mv k8s/namespace.yaml          k8s/base/namespace.yaml
git mv k8s/network-policy.yaml     k8s/base/network-policy.yaml
git mv k8s/backend/service.yaml    k8s/base/backend/service.yaml
git mv k8s/backend/hpa.yaml        k8s/base/backend/hpa.yaml
git mv k8s/backend/pdb.yaml        k8s/base/backend/pdb.yaml
git mv k8s/frontend/service.yaml   k8s/base/frontend/service.yaml
git mv k8s/frontend/hpa.yaml       k8s/base/frontend/hpa.yaml
git mv k8s/backend/deployment.yaml k8s/base/backend/deployment.yaml
git mv k8s/frontend/deployment.yaml k8s/base/frontend/deployment.yaml
git mv k8s/ingress.yaml            k8s/base/ingress.yaml
git mv k8s/external-secrets/backend-secrets.yaml k8s/base/external-secrets/backend-secrets.yaml
git rm k8s/external-secrets/secret-store.yaml
```

- [ ] **Step 2: backend deployment 편집 — bare image name + sync-wave**

`k8s/base/backend/deployment.yaml`에서 image 라인과 어노테이션 수정:

`metadata`에 sync-wave 추가, image를 bare name으로:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: student-mgmt
  annotations:
    argocd.argoproj.io/sync-wave: "1"
spec:
  # ... (기존 spec 유지) ...
  template:
    # ...
    spec:
      containers:
      - name: backend
        image: student-mgmt/backend   # kustomize images 트랜스포머가 ECR URL+태그로 치환
```
(나머지 필드는 기존과 동일하게 유지. `${ECR_BACKEND_IMAGE}:${IMAGE_TAG}` → `student-mgmt/backend`)

- [ ] **Step 3: frontend deployment 편집**

`k8s/base/frontend/deployment.yaml`:
```yaml
metadata:
  name: frontend
  namespace: student-mgmt
  annotations:
    argocd.argoproj.io/sync-wave: "1"
```
image 라인: `image: student-mgmt/frontend` (`${ECR_FRONTEND_IMAGE}:${IMAGE_TAG}` 치환)

- [ ] **Step 4: ingress 편집 — 플레이스홀더 제거 + sync-wave**

`k8s/base/ingress.yaml` annotations에서 `certificate-arn`, `group.name`, `external-dns ... hostname` **3줄 삭제**(overlay 패치로 주입), `sync-wave: "2"` 추가:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: main
  namespace: student-mgmt
  annotations:
    argocd.argoproj.io/sync-wave: "2"
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP":80},{"HTTPS":443}]'
    alb.ingress.kubernetes.io/ssl-redirect: "443"
spec:
  ingressClassName: alb
  rules:
  # ... (기존 rules 그대로 유지) ...
```

- [ ] **Step 5: ExternalSecret 편집 — sync-wave**

`k8s/base/external-secrets/backend-secrets.yaml`의 `metadata`에 추가(나머지 동일):
```yaml
metadata:
  name: backend-secrets
  namespace: student-mgmt
  annotations:
    argocd.argoproj.io/sync-wave: "0"
```

- [ ] **Step 6: base kustomization.yaml 작성**

`k8s/base/kustomization.yaml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - namespace.yaml
  - network-policy.yaml
  - external-secrets/backend-secrets.yaml
  - backend/deployment.yaml
  - backend/service.yaml
  - backend/hpa.yaml
  - backend/pdb.yaml
  - frontend/deployment.yaml
  - frontend/service.yaml
  - frontend/hpa.yaml
  - ingress.yaml
```

- [ ] **Step 7: base 렌더 검증**

Run:
```bash
kubectl kustomize k8s/base > /tmp/base.yaml && \
grep -q "image: student-mgmt/backend" /tmp/base.yaml && \
grep -q 'sync-wave: "2"' /tmp/base.yaml && \
! grep -q '\${' /tmp/base.yaml && echo "BASE OK"
```
Expected: `BASE OK` 출력(렌더 성공, 플레이스홀더 잔존 없음).

- [ ] **Step 8: 커밋**

```bash
git add k8s/base
git commit -m "[refactor] k8s 매니페스트를 Kustomize base로 재편 (envsubst 플레이스홀더 제거)"
```

---

## Task 2: 환경별 overlay 생성 (dev/staging/prod)

**Files:**
- Create: `k8s/overlays/{dev,staging,prod}/kustomization.yaml`
- Create: `k8s/overlays/{dev,staging,prod}/ingress-patch.yaml`
- Create: `k8s/overlays/{dev,staging,prod}/externalsecret-patch.yaml`

> 아래 dev 예시를 staging/prod에 동일 구조로 작성하되, `namespace`/`group.name`/`hostname`/`remoteRef.key`의 환경명(dev→staging→prod)만 바꾼다. `newName`(ECR URL)과 `certificate-arn`(와일드카드)은 세 환경 공통.

- [ ] **Step 1: dev kustomization.yaml**

`k8s/overlays/dev/kustomization.yaml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: student-mgmt-dev
resources:
  - ../../base
images:
  - name: student-mgmt/backend
    newName: <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/student-mgmt/backend
    newTag: latest      # CI write-back이 sha-<gitsha>로 갱신
  - name: student-mgmt/frontend
    newName: <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/student-mgmt/frontend
    newTag: latest
patches:
  - path: ingress-patch.yaml
  - path: externalsecret-patch.yaml
```
(`<ACCOUNT_ID>` = `aws sts get-caller-identity --query Account --output text` 결과로 치환)

- [ ] **Step 2: dev ingress-patch.yaml**

`k8s/overlays/dev/ingress-patch.yaml`:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: main
  annotations:
    alb.ingress.kubernetes.io/certificate-arn: "<ACM_CERT_ARN>"   # terraform output acm_cert_arn (3환경 공통)
    alb.ingress.kubernetes.io/group.name: student-mgmt-dev
    external-dns.alpha.kubernetes.io/hostname: "dev.<domain>"
```

- [ ] **Step 3: dev externalsecret-patch.yaml**

`k8s/overlays/dev/externalsecret-patch.yaml` (CRD 리스트는 atomic replace → 전체 data 제공):
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: backend-secrets
spec:
  data:
    - secretKey: DB_USERNAME
      remoteRef: { key: student-mgmt/dev/db, property: username }
    - secretKey: DB_PASSWORD
      remoteRef: { key: student-mgmt/dev/db, property: password }
    - secretKey: SPRING_DATASOURCE_URL
      remoteRef: { key: student-mgmt/dev/db, property: jdbc_url }
    - secretKey: JWT_SECRET
      remoteRef: { key: student-mgmt/dev/jwt, property: secret }
```

- [ ] **Step 4: staging overlay 생성**

`k8s/overlays/staging/` 에 Step 1~3과 동일 구조로 생성하되 치환:
- kustomization.yaml: `namespace: student-mgmt-staging` (images 블록 동일)
- ingress-patch.yaml: `group.name: student-mgmt-staging`, `hostname: "staging.<domain>"`, cert 동일
- externalsecret-patch.yaml: 모든 `key`를 `student-mgmt/staging/db` · `student-mgmt/staging/jwt`로

- [ ] **Step 5: prod overlay 생성**

`k8s/overlays/prod/` 동일 구조:
- kustomization.yaml: `namespace: student-mgmt-prod`
- ingress-patch.yaml: `group.name: student-mgmt-prod`, `hostname: "app.<domain>"`, cert 동일
- externalsecret-patch.yaml: `student-mgmt/prod/db` · `student-mgmt/prod/jwt`

- [ ] **Step 6: 3개 overlay 렌더 검증**

Run:
```bash
for env in dev staging prod; do
  kubectl kustomize k8s/overlays/$env > /tmp/$env.yaml || { echo "$env BUILD FAIL"; exit 1; }
  grep -q "namespace: student-mgmt-$env" /tmp/$env.yaml || { echo "$env NS FAIL"; exit 1; }
  grep -q "ecr.ap-northeast-2.amazonaws.com/student-mgmt/backend" /tmp/$env.yaml || { echo "$env IMG FAIL"; exit 1; }
  ! grep -q '\${' /tmp/$env.yaml || { echo "$env PLACEHOLDER FAIL"; exit 1; }
  echo "$env OK"
done
```
Expected: `dev OK` / `staging OK` / `prod OK`.

- [ ] **Step 7: 커밋**

```bash
git add k8s/overlays
git commit -m "[feat] dev/staging/prod Kustomize overlay 추가"
```

---

## Task 3: Argo bootstrap 로컬 helm 차트

**Files:**
- Create: `k8s/bootstrap/Chart.yaml`, `k8s/bootstrap/values.yaml`
- Create: `k8s/bootstrap/templates/{appproject,applicationset,clustersecretstore,argocd-admin-externalsecret,argocd-repo-externalsecret}.yaml`

- [ ] **Step 1: Chart.yaml / values.yaml**

`k8s/bootstrap/Chart.yaml`:
```yaml
apiVersion: v2
name: argocd-bootstrap
description: Argo CD AppProject/ApplicationSet/ESO bootstrap for student-mgmt
version: 0.1.0
```
`k8s/bootstrap/values.yaml`:
```yaml
repoURL: ""        # Terraform이 주입: https://github.com/<org>/<repo>.git
targetRevision: main
region: ap-northeast-2
environments:
  - dev
  - staging
  - prod
```

- [ ] **Step 2: ClusterSecretStore 템플릿**

`k8s/bootstrap/templates/clustersecretstore.yaml` (기존 secret-store.yaml 내용 이전, region 파라미터화):
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ClusterSecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: {{ .Values.region }}
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets
            namespace: external-secrets
```

- [ ] **Step 3: AppProject 템플릿**

`k8s/bootstrap/templates/appproject.yaml`:
```yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: student-mgmt
  namespace: argocd
spec:
  description: student-mgmt 워크로드
  sourceRepos:
    - {{ .Values.repoURL | quote }}
  destinations:
{{- range .Values.environments }}
    - server: https://kubernetes.default.svc
      namespace: student-mgmt-{{ . }}
{{- end }}
  clusterResourceWhitelist:
    - group: "*"
      kind: "*"
  namespaceResourceWhitelist:
    - group: "*"
      kind: "*"
```

- [ ] **Step 4: ApplicationSet 템플릿**

`k8s/bootstrap/templates/applicationset.yaml`:
```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: student-mgmt
  namespace: argocd
spec:
  goTemplate: true
  generators:
    - list:
        elements:
{{- range .Values.environments }}
          - env: {{ . }}
{{- end }}
  template:
    metadata:
      name: 'student-mgmt-{{`{{.env}}`}}'
    spec:
      project: student-mgmt
      source:
        repoURL: {{ .Values.repoURL | quote }}
        targetRevision: {{ .Values.targetRevision }}
        path: 'k8s/overlays/{{`{{.env}}`}}'
      destination:
        server: https://kubernetes.default.svc
        namespace: 'student-mgmt-{{`{{.env}}`}}'
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        syncOptions:
          - CreateNamespace=true
          - ServerSideApply=true
```
> prod를 수동 sync로 바꾸려면 별도 template(matrix/조건)로 분리하거나 prod Application의 `automated`를 제거. 현재는 3환경 자동, 승격 PR이 게이트.

- [ ] **Step 5: Argo admin 비밀번호 ExternalSecret**

`k8s/bootstrap/templates/argocd-admin-externalsecret.yaml` (argocd-secret에 merge):
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: argocd-admin
  namespace: argocd
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: argocd-secret
    creationPolicy: Merge      # 차트가 만든 argocd-secret에 admin 키만 병합
  data:
    - secretKey: admin.password
      remoteRef: { key: student-mgmt/argocd/admin, property: passwordBcrypt }
    - secretKey: admin.passwordMtime
      remoteRef: { key: student-mgmt/argocd/admin, property: passwordMtime }
```

- [ ] **Step 6: Argo private repo 자격증명 ExternalSecret**

`k8s/bootstrap/templates/argocd-repo-externalsecret.yaml` (private 레포 pull용; public이면 이 템플릿 삭제):
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: argocd-repo
  namespace: argocd
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: argocd-repo-student-mgmt
    creationPolicy: Owner
    template:
      metadata:
        labels:
          argocd.argoproj.io/secret-type: repository
  data:
    - secretKey: url
      remoteRef: { key: student-mgmt/argocd/repo, property: url }
    - secretKey: username
      remoteRef: { key: student-mgmt/argocd/repo, property: username }
    - secretKey: password
      remoteRef: { key: student-mgmt/argocd/repo, property: token }
```

- [ ] **Step 7: 차트 렌더 검증**

Run:
```bash
helm template argocd-bootstrap k8s/bootstrap \
  --set repoURL=https://github.com/org/repo.git > /tmp/boot.yaml && \
grep -q "kind: ApplicationSet" /tmp/boot.yaml && \
grep -q "student-mgmt-prod" /tmp/boot.yaml && \
grep -q "kind: ClusterSecretStore" /tmp/boot.yaml && echo "BOOTSTRAP OK"
```
Expected: `BOOTSTRAP OK`.

- [ ] **Step 8: 커밋**

```bash
git add k8s/bootstrap
git commit -m "[feat] Argo bootstrap helm 차트 추가 (AppProject/ApplicationSet/ESO)"
```

---

## Task 4: Terraform — Argo CD + bootstrap helm_release

**Files:**
- Modify: `infra/terraform/eks.tf` (Helm Releases 섹션 끝에 추가)
- Modify: `infra/terraform/variables.tf` (argocd 호스트 변수)

- [ ] **Step 1: variables.tf에 argocd 호스트 변수 추가**

`infra/terraform/variables.tf` 끝에:
```hcl
variable "argocd_chart_version" {
  description = "argo-cd helm 차트 버전"
  type        = string
  default     = "7.7.7"
}
```

- [ ] **Step 2: eks.tf에 argocd helm_release 추가**

`infra/terraform/eks.tf` 파일 끝(external_dns 아래)에 추가:
```hcl
resource "helm_release" "argocd" {
  name             = "argocd"
  repository       = "https://argoproj.github.io/argo-helm"
  chart            = "argo-cd"
  namespace        = "argocd"
  create_namespace = true
  version          = var.argocd_chart_version

  values = [yamlencode({
    configs = {
      params = { "server.insecure" = true }   # ALB가 TLS 종료
      cm = {
        # ExternalSecret Ready 헬스체크 (wave 0 게이팅)
        "resource.customizations.health.external-secrets.io_ExternalSecret" = <<-EOT
          hs = {}
          if obj.status ~= nil and obj.status.conditions ~= nil then
            for _, c in ipairs(obj.status.conditions) do
              if c.type == "Ready" and c.status == "True" then
                hs.status = "Healthy"; hs.message = c.message; return hs
              end
            end
          end
          hs.status = "Progressing"; hs.message = "Waiting for ExternalSecret to be Ready"
          return hs
        EOT
      }
    }
    server = {
      ingress = {
        enabled          = true
        ingressClassName = "alb"
        hostname         = "argocd.${var.domain_name}"
        annotations = {
          "alb.ingress.kubernetes.io/scheme"                  = "internet-facing"
          "alb.ingress.kubernetes.io/target-type"             = "ip"
          "alb.ingress.kubernetes.io/listen-ports"            = "[{\"HTTP\":80},{\"HTTPS\":443}]"
          "alb.ingress.kubernetes.io/ssl-redirect"            = "443"
          "alb.ingress.kubernetes.io/certificate-arn"         = aws_acm_certificate_validation.main.certificate_arn
          "alb.ingress.kubernetes.io/group.name"              = "argocd"
          "external-dns.alpha.kubernetes.io/hostname"         = "argocd.${var.domain_name}"
        }
      }
    }
  })]

  depends_on = [module.eks, helm_release.aws_lbc]
}

resource "helm_release" "argocd_bootstrap" {
  name      = "argocd-bootstrap"
  chart     = "${path.module}/../../k8s/bootstrap"
  namespace = "argocd"

  set { name = "repoURL"; value = "https://github.com/${var.github_org}/${var.github_repo}.git" }
  set { name = "region";  value = var.aws_region }

  depends_on = [helm_release.argocd, helm_release.external_secrets]
}
```

- [ ] **Step 3: Terraform 포맷/검증**

Run:
```bash
cd infra/terraform
terraform fmt -check
terraform init -backend=false
terraform validate
```
Expected: `terraform validate` → `Success! The configuration is valid.` (fmt 실패 시 `terraform fmt` 후 재실행)

- [ ] **Step 4: 커밋**

```bash
git add infra/terraform/eks.tf infra/terraform/variables.tf
git commit -m "[feat] Terraform으로 Argo CD + bootstrap 차트 설치"
```

---

## Task 5: Terraform — CI 클러스터 권한 제거 (보안 핵심)

**Files:**
- Modify: `infra/terraform/eks.tf` (access_entries 제거)
- Modify: `infra/terraform/iam.tf` (eks:DescribeCluster 정책 제거)

- [ ] **Step 1: eks.tf의 github_actions access entry 제거**

`infra/terraform/eks.tf`의 module "eks" 블록에서 아래 전체 삭제(`enable_cluster_creator_admin_permissions = true` 다음 줄들):
```hcl
  # GitHub Actions Role에 EKS 관리자 권한 부여
  access_entries = {
    github_actions = {
      principal_arn = aws_iam_role.github_actions.arn
      policy_associations = {
        admin = {
          policy_arn   = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = { type = "cluster" }
        }
      }
    }
  }
```

- [ ] **Step 2: iam.tf의 eks:DescribeCluster 정책 제거**

`infra/terraform/iam.tf`에서 아래 리소스 전체 삭제:
```hcl
# kubectl 사용을 위한 EKS 클러스터 조회 권한
resource "aws_iam_role_policy" "github_actions_eks" {
  name = "${var.project}-github-actions-eks"
  role = aws_iam_role.github_actions.name
  policy = jsonencode({ ... })
}
```
(ECR PowerUser attachment `github_actions_ecr`는 **유지**)

- [ ] **Step 3: 검증**

Run:
```bash
cd infra/terraform
terraform fmt -check && terraform validate && \
! grep -q "AmazonEKSClusterAdminPolicy" eks.tf && \
! grep -q "github_actions_eks" iam.tf && echo "IAM REDUCED OK"
```
Expected: `Success! The configuration is valid.` + `IAM REDUCED OK`

- [ ] **Step 4: 커밋**

```bash
git add infra/terraform/eks.tf infra/terraform/iam.tf
git commit -m "[refactor] GitHub Actions 역할에서 EKS 클러스터 권한 제거 (GitOps 전환)"
```

---

## Task 6: Terraform — 환경별 Secrets Manager 항목

**Files:**
- Modify: `infra/terraform/secrets.tf`

> 참고: ESO IRSA 정책은 이미 `student-mgmt/*`를 허용하므로 IAM 변경 불필요. 물리 MySQL 데이터베이스(스키마) 생성은 private RDS 접근 제약으로 Terraform이 직접 수행하지 않는다 — Task 11에서 클러스터 내부 Job/수동 SQL로 생성한다.

- [ ] **Step 1: 기존 secrets.tf 확인**

Run: `cat infra/terraform/secrets.tf`
기존 `student-mgmt/db`, `student-mgmt/jwt` 시크릿 리소스 구조 파악(아래 Step 2는 그 패턴을 따른다).

- [ ] **Step 2: 환경별 시크릿 + Argo 시크릿 추가**

`infra/terraform/secrets.tf`에 추가(기존 리소스 네이밍 패턴에 맞춤; `for_each`로 3환경):
```hcl
locals {
  envs = ["dev", "staging", "prod"]
}

# 환경별 DB 자격증명 (값은 콘솔/별도 프로세스로 채움 — placeholder만 생성)
resource "aws_secretsmanager_secret" "env_db" {
  for_each = toset(local.envs)
  name     = "${var.project}/${each.value}/db"
}

resource "aws_secretsmanager_secret" "env_jwt" {
  for_each = toset(local.envs)
  name     = "${var.project}/${each.value}/jwt"
}

# Argo admin 비밀번호 (passwordBcrypt, passwordMtime) — 값은 Task 11에서 주입
resource "aws_secretsmanager_secret" "argocd_admin" {
  name = "${var.project}/argocd/admin"
}

# Argo private repo 자격증명 (public 레포면 생략)
resource "aws_secretsmanager_secret" "argocd_repo" {
  name = "${var.project}/argocd/repo"
}
```

- [ ] **Step 3: 검증**

Run:
```bash
cd infra/terraform && terraform fmt -check && terraform init -backend=false && terraform validate
```
Expected: `Success! The configuration is valid.`

- [ ] **Step 4: 커밋**

```bash
git add infra/terraform/secrets.tf
git commit -m "[feat] 환경별 + Argo Secrets Manager 시크릿 추가"
```

---

## Task 7: CI — cd.yml write-back 전환

**Files:**
- Modify: `.github/workflows/cd.yml`

- [ ] **Step 1: 트리거에 paths-ignore 추가 + deploy 잡 제거**

`.github/workflows/cd.yml`의 `on:` 블록 수정(매니페스트 커밋이 빌드를 재트리거하지 않도록):
```yaml
on:
  push:
    branches: [main]
    tags:
      - 'v*.*.*'
    paths-ignore:
      - 'k8s/**'
      - 'docs/**'
```
`permissions`에 `contents: write` 추가:
```yaml
permissions:
  id-token: write
  contents: write   # write-back 커밋용
```
그리고 `deploy:` 잡(79~138줄) **전체 삭제**.

- [ ] **Step 2: build-and-push 잡에 write-back 스텝 추가**

`build-and-push` 잡 마지막(frontend push 뒤)에 추가:
```yaml
      - name: Set up kustomize
        run: |
          curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
          sudo mv kustomize /usr/local/bin/

      - name: Write-back image tag to dev overlay
        run: |
          cd k8s/overlays/dev
          kustomize edit set image \
            student-mgmt/backend=${{ steps.ecr_login.outputs.registry }}/student-mgmt/backend:${{ steps.meta.outputs.version }} \
            student-mgmt/frontend=${{ steps.ecr_login.outputs.registry }}/student-mgmt/frontend:${{ steps.meta.outputs.version }}

      - name: Commit write-back
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add k8s/overlays/dev/kustomization.yaml
          git commit -m "[chore] dev 이미지 태그 갱신 → ${{ steps.meta.outputs.version }}" || echo "no changes"
          git push
```
> `GITHUB_TOKEN`이 author인 커밋은 워크플로우를 재트리거하지 않으므로(GitHub 기본) `paths-ignore`와 함께 2중 루프 방지.

- [ ] **Step 3: 워크플로우 YAML 검증**

Run:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/cd.yml')); print('YAML OK')" && \
! grep -q "update-kubeconfig" .github/workflows/cd.yml && \
! grep -q "rollout status" .github/workflows/cd.yml && echo "CD CUTOVER OK"
```
Expected: `YAML OK` + `CD CUTOVER OK` (kubectl/rollout 잔재 없음). `actionlint` 설치 시 추가 실행 권장.

- [ ] **Step 4: 커밋**

```bash
git add .github/workflows/cd.yml
git commit -m "[refactor] cd.yml을 ECR push + 이미지 태그 write-back으로 전환 (kubectl 제거)"
```

---

## Task 8: CI — 승격(promotion) 워크플로우

**Files:**
- Create: `.github/workflows/promote.yml`

- [ ] **Step 1: promote.yml 작성**

`.github/workflows/promote.yml` (dev→staging→prod 태그 복사 PR 생성):
```yaml
name: Promote
on:
  workflow_dispatch:
    inputs:
      from:
        description: "원본 환경"
        type: choice
        options: [dev, staging]
        default: dev
      to:
        description: "대상 환경"
        type: choice
        options: [staging, prod]
        default: staging

permissions:
  contents: write
  pull-requests: write

jobs:
  promote:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up kustomize
        run: |
          curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
          sudo mv kustomize /usr/local/bin/

      - name: Copy pinned images from source to target overlay
        run: |
          BE=$(cd k8s/overlays/${{ inputs.from }} && kustomize edit set image --help >/dev/null 2>&1; \
               grep -A2 "name: student-mgmt/backend" kustomization.yaml | grep -E "newName|newTag" | paste -sd: -)
          # 견고한 방식: yq로 source의 images를 읽어 target에 적용
          for svc in backend frontend; do
            NEWNAME=$(yq ".images[] | select(.name==\"student-mgmt/$svc\") | .newName" k8s/overlays/${{ inputs.from }}/kustomization.yaml)
            NEWTAG=$(yq ".images[] | select(.name==\"student-mgmt/$svc\") | .newTag" k8s/overlays/${{ inputs.from }}/kustomization.yaml)
            (cd k8s/overlays/${{ inputs.to }} && kustomize edit set image student-mgmt/$svc=$NEWNAME:$NEWTAG)
          done

      - name: Create promotion PR
        uses: peter-evans/create-pull-request@v6
        with:
          branch: promote/${{ inputs.from }}-to-${{ inputs.to }}
          title: "[promote] ${{ inputs.from }} → ${{ inputs.to }} 이미지 승격"
          body: "Argo가 ${{ inputs.to }} 환경을 sync합니다. 머지가 배포 게이트입니다."
          commit-message: "[chore] ${{ inputs.to }} 이미지 태그를 ${{ inputs.from }}에서 승격"
          add-paths: k8s/overlays/${{ inputs.to }}/kustomization.yaml
```
> `yq`는 ubuntu-latest 러너에 기본 설치됨. PR 머지가 승격 게이트.

- [ ] **Step 2: YAML 검증**

Run:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/promote.yml')); print('PROMOTE YAML OK')"
```
Expected: `PROMOTE YAML OK`

- [ ] **Step 3: 커밋**

```bash
git add .github/workflows/promote.yml
git commit -m "[feat] 환경 승격 워크플로우 추가 (workflow_dispatch → PR)"
```

---

## Task 9: 레거시 정리 + 문서 갱신

**Files:**
- Delete: `appspec.yml`, `docker-compose.prod.yml`, `scripts/{ec2-setup,start,stop,validate}.sh`
- Modify: `CLAUDE.md` (CI/CD 섹션)

- [ ] **Step 1: CodeDeploy 잔재 삭제 확정**

Run:
```bash
git rm -f appspec.yml docker-compose.prod.yml scripts/ec2-setup.sh scripts/start.sh scripts/stop.sh scripts/validate.sh 2>/dev/null || true
git status --short
```
(이미 삭제 staged 상태이면 확정만 됨)

- [ ] **Step 2: CLAUDE.md CI/CD 섹션 갱신**

`CLAUDE.md`의 "## CI/CD" 섹션을 GitOps 흐름으로 교체:
```markdown
## CI/CD

- **CI** (`.github/workflows/ci.yml`): `main`/`develop` push/PR 시 MySQL 컨테이너로 테스트 → JAR 빌드.
- **CD** (`.github/workflows/cd.yml`): `main` push 시 ECR에 백엔드/프론트 이미지 push 후, `kustomize edit set image`로 `k8s/overlays/dev`의 태그를 갱신·커밋(write-back). **클러스터 자격증명 없음.**
- **GitOps**: Argo CD(EKS `argocd` 네임스페이스)가 git을 watch하여 `k8s/overlays/{env}`를 각 `student-mgmt-{env}` 네임스페이스에 동기화. dev는 자동, staging/prod는 `promote.yml`이 생성하는 PR 머지로 승격.
- **부트스트랩**: Argo CD/ApplicationSet/AppProject/ClusterSecretStore는 Terraform `helm_release`(`infra/terraform/eks.tf` + `k8s/bootstrap/` 로컬 차트)로 설치.
```

- [ ] **Step 3: 커밋**

```bash
git add -A
git commit -m "[chore] CodeDeploy 잔재 제거 및 CLAUDE.md CI/CD 문서 갱신"
```

---

## Task 10: 전체 정적 검증 (게이트)

**Files:** 없음 (검증만)

- [ ] **Step 1: 모든 overlay + bootstrap + terraform 일괄 검증**

Run:
```bash
set -e
for env in dev staging prod; do kubectl kustomize k8s/overlays/$env >/dev/null && echo "kustomize $env OK"; done
helm template b k8s/bootstrap --set repoURL=https://github.com/org/repo.git >/dev/null && echo "helm OK"
( cd infra/terraform && terraform init -backend=false >/dev/null && terraform validate )
for f in .github/workflows/cd.yml .github/workflows/promote.yml; do
  python3 -c "import yaml; yaml.safe_load(open('$f'))" && echo "$f OK"
done
echo "ALL STATIC CHECKS PASSED"
```
Expected: 모든 라인 OK + `ALL STATIC CHECKS PASSED`.

---

## Task 11: 라이브 컷오버 & 검증 (수동 — AWS 자격증명 필요)

> 이 Task는 실제 AWS/클러스터에 적용한다. 자동 검증 불가. PR 머지 전 `feature/EP-XX-argocd-gitops` 브랜치에서 수행하거나, 머지 후 운영자가 실행.

**운영 전제조건 (코드 리뷰에서 발견 — 미충족 시 런타임 실패):**
- **`main` 브랜치 보호 규칙**: cd.yml write-back은 `github-actions[bot]`이 `main`에 직접 push한다. 브랜치 보호가 직접 push를 막으면 403으로 실패 → `github-actions[bot]` 예외 허용, 또는 PAT/GitHub App 토큰 사용, 또는 write-back을 별도 브랜치+PR로 변경 필요.
- **"Allow GitHub Actions to create pull requests" 설정**: promote.yml이 PR을 생성하려면 repo/org Settings → Actions에서 이 옵션이 켜져 있어야 한다.
- **태그(`v*`) push 주의**: cd.yml의 `paths-ignore`는 태그 push에도 적용되어, k8s 파일만 바뀐 커밋에 태그를 달면 이미지 빌드가 스킵될 수 있다. 릴리스 태그는 앱 코드 변경 커밋에 부여할 것.

- [ ] **Step 1: 환경 시크릿 값 주입**

```bash
# 환경별 DB/JWT (예: dev)
aws secretsmanager put-secret-value --secret-id student-mgmt/dev/db \
  --secret-string '{"username":"appuser","password":"<pw>","jdbc_url":"jdbc:mysql://<rds>:3306/student_mgmt_dev"}'
aws secretsmanager put-secret-value --secret-id student-mgmt/dev/jwt \
  --secret-string '{"secret":"<256bit-secret>"}'
# staging/prod 동일 (db 이름 student_mgmt_staging / student_mgmt_prod)

# Argo admin 비밀번호 (bcrypt)
HASH=$(htpasswd -nbBC 10 "" '<admin-pw>' | tr -d ':\n' | sed 's/$2y/$2a/')
aws secretsmanager put-secret-value --secret-id student-mgmt/argocd/admin \
  --secret-string "{\"passwordBcrypt\":\"$HASH\",\"passwordMtime\":\"$(date +%FT%T%Z)\"}"

# Argo repo 자격증명 (private 레포)
aws secretsmanager put-secret-value --secret-id student-mgmt/argocd/repo \
  --secret-string '{"url":"https://github.com/<org>/<repo>.git","username":"<gh-user>","token":"<PAT>"}'
```

- [ ] **Step 2: MySQL 환경별 데이터베이스 생성**

클러스터 내부에서 1회(RDS가 private이므로):
```bash
kubectl run mysql-init --rm -it --image=mysql:8 -n student-mgmt-dev --restart=Never -- \
  mysql -h <rds-endpoint> -u <admin> -p<pw> -e \
  "CREATE DATABASE IF NOT EXISTS student_mgmt_dev; CREATE DATABASE IF NOT EXISTS student_mgmt_staging; CREATE DATABASE IF NOT EXISTS student_mgmt_prod;"
```

- [ ] **Step 3: Terraform apply (Argo 설치)**

```bash
cd infra/terraform
terraform apply -target=module.eks   # (이미 클러스터 존재 시 생략 가능)
terraform apply                      # 애드온 + argocd + argocd_bootstrap
```

- [ ] **Step 4: overlay의 `<ACCOUNT_ID>`/`<ACM_CERT_ARN>`/`<domain>` 실제값 커밋**

```bash
ACC=$(aws sts get-caller-identity --query Account --output text)
CERT=$(terraform -chdir=infra/terraform output -raw acm_cert_arn)
# k8s/overlays/*/kustomization.yaml의 newName, ingress-patch.yaml의 certificate-arn/hostname을 실제값으로 치환 후 커밋
```

- [ ] **Step 5: Argo 동기화 확인**

```bash
kubectl -n argocd get applications
# student-mgmt-dev/staging/prod 모두 Synced / Healthy 기대
kubectl -n argocd get applicationset student-mgmt -o yaml | grep -A3 status
# Argo UI: https://argocd.<domain> (admin / Step1 비밀번호)
```
Expected: 3개 Application Synced+Healthy, sync wave 순서(ExternalSecret→워크로드→Ingress) 정상.

- [ ] **Step 6: end-to-end 검증**

앱 코드 변경을 `main`에 push → cd.yml이 ECR push + dev overlay write-back 커밋 → Argo dev 자동 sync 확인. 이후 Promote 워크플로우 실행 → staging PR 머지 → staging sync 확인.

- [ ] **Step 7: 브랜치 정리**

`finishing-a-development-branch` 스킬로 머지/PR 처리.

---

## Self-Review (작성자 점검 결과)

- **Spec 커버리지**: 5.1 base/overlay→Task1·2, 5.2 write-back/승격→Task7·8, 5.3 Argo설치/부트스트랩→Task3·4, 5.4 UI노출/인증→Task4(ingress)·Task3(admin ESO), 5.5 ApplicationSet/wave/health→Task3·4, 5.6 CI권한축소→Task5·Task7, 데이터레이어→Task6·Task11. 누락 없음.
- **플레이스홀더**: 환경 고유값(`<ACCOUNT_ID>` 등)은 "사전 준비"에 출처 명시. 코드 placeholder 아님.
- **타입/이름 일관성**: kustomize image name `student-mgmt/backend|frontend`가 base·overlay·cd.yml·promote.yml 전반 일치. 네임스페이스 `student-mgmt-{env}`, 시크릿 경로 `student-mgmt/{env}/db|jwt`, Argo 시크릿 경로 `student-mgmt/argocd/{admin,repo}` 일관.
- **추가 발견**: private 레포 pull용 Argo repo 자격증명(spec 미명시)을 Task3 Step6 + Task6에 추가. 물리 MySQL DB 생성은 private RDS 제약으로 Task11 클러스터 내부 Job으로 분리.
