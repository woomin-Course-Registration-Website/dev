# 시스템 전면 재설계 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 스택을 Amplify + API Gateway + private ALB + EKS + Sealed Secrets로 재편하고, 미사용 인프라(CloudFront/WAF/Secrets Manager/ESO/Fluent Bit/CloudWatch alarms)를 전부 제거한다.

**Architecture:** Phase 1에서 옛 인프라를 모두 들어내고(Terraform 파일 삭제·k8s 매니페스트 정리), Phase 2에서 새 컴포넌트(Sealed Secrets controller, Amplify, API Gateway × 3, private ALB)를 추가한다. Phase 3은 정적 검증과 문서 재작성, Phase 4는 운영자 컷오버 런북.

**Tech Stack:** AWS Amplify, API Gateway HTTP API, ALB(internal), EKS 1.30, RDS MySQL 8, Bitnami Sealed Secrets, kube-prometheus-stack, Argo CD, Kustomize, Terraform.

---

## 사전 준비 / 전제

- **작업 브랜치 결정(사용자)**: feature/argocd-gitops 위에서 변경 커밋 누적 / 새 브랜치로 cherry-pick / hard reset 중 택1. 본 계획은 어느 경우에도 적용 가능(매 Task가 독립 커밋).
- **검증 도구**: `kubectl` v1.34, `helm` v4.2, `terraform` v1.15, `python` 3.13. 모두 설치 확인됨.
- **YAML lint은 UTF-8 명시**: Windows Python 기본 cp949에서 한글 주석이 깨지므로 `PYTHONUTF8=1 python -c "yaml.safe_load(open(f, encoding='utf-8'))"` 패턴 사용.
- **이 계획은 코드만 다룬다**. 실제 AWS apply·시크릿 주입·DB 스키마 생성은 Task 21(컷오버 런북)에서 운영자가 수동 실행.
- **환경 고유값 치환**: `<ACCOUNT_ID>`, `<ACM_CERT_ARN>`, `<domain>` 등은 spec의 동일 토큰을 그대로 유지(컷오버 단계에서 실제값 주입).

---

## File Structure

**삭제**
- `infra/terraform/cloudfront.tf`
- `infra/terraform/waf.tf`
- `infra/terraform/secrets.tf`
- `infra/terraform/monitoring.tf`
- `k8s/base/external-secrets/` (디렉토리 전체)
- `k8s/base/frontend/` (디렉토리 전체)
- `k8s/bootstrap/templates/clustersecretstore.yaml`
- `k8s/bootstrap/templates/argocd-admin-externalsecret.yaml`
- `k8s/bootstrap/templates/argocd-repo-externalsecret.yaml`

**생성**
- `infra/terraform/amplify.tf`
- `infra/terraform/apigateway.tf`
- `amplify.yml` (repo root)
- (운영자가 컷오버 시) `k8s/overlays/{dev,staging,prod}/sealed-secrets/backend-secrets.yaml`

**수정**
- `infra/terraform/eks.tf` — ESO·Fluent Bit helm_release/IRSA 제거, Sealed Secrets helm_release 추가, ALB SG 입력 허용
- `infra/terraform/iam.tf` — `aws_iam_role_policy "eso_secrets_manager"` 제거
- `infra/terraform/acm.tf` — us-east-1 인증서·검증 리소스 제거
- `infra/terraform/ecr.tf` — `student-mgmt/frontend` repo 제거
- `infra/terraform/variables.tf` — `amplify_github_token`, `sealed_secrets_chart_version` 추가, `cluster_endpoint_public_access_cidrs` 유지
- `infra/terraform/outputs.tf` — amplify·apigateway 출력 추가, cloudfront 출력 제거
- `k8s/base/kustomization.yaml` — frontend·external-secrets 리소스 항목 제거
- `k8s/base/ingress.yaml` — backend 단독 경로, `scheme: internal`
- `k8s/base/network-policy.yaml` — frontend 관련 전부 제거
- `k8s/overlays/{dev,staging,prod}/kustomization.yaml` — frontend image 제거, sealed-secrets 리소스 추가
- `k8s/overlays/{dev,staging,prod}/ingress-patch.yaml` — external-dns/hostname/group.name 어노테이션 제거(API Gateway가 도메인 처리)
- 삭제: `k8s/overlays/{dev,staging,prod}/externalsecret-patch.yaml`
- `.github/workflows/cd.yml` — frontend 빌드/푸시 스텝 제거, `paths-ignore`에 `frontend/**` 추가
- `CLAUDE.md`, `AGENTS.md` — CI/CD 섹션 갱신
- `docs/SYSTEM_ARCHITECTURE.md`, `docs/INFRASTRUCTURE.md` — 새 아키텍처로 전면 재작성

---

## Task 1: Terraform — CloudFront/WAF 제거

**Files:**
- Delete: `infra/terraform/cloudfront.tf`, `infra/terraform/waf.tf`

- [ ] **Step 1: 두 파일 삭제 + 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm infra/terraform/cloudfront.tf infra/terraform/waf.tf
cd infra/terraform
terraform fmt -check
terraform init -backend=false >/dev/null
terraform validate
```
Expected: `Success! The configuration is valid.`

만약 `outputs.tf`에 `cloudfront_domain` 출력이 남아 있어 validate 실패하면 → 그 출력 줄을 삭제 후 다시 validate. 다른 파일에서 `aws_cloudfront_distribution.main`이나 `aws_wafv2_web_acl.cloudfront`나 `aws_s3_bucket.cf_logs` 등을 참조하는 곳이 있으면 모두 제거.

- [ ] **Step 2: 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git add -A infra/terraform/
git commit -m "[refactor] terraform: CloudFront/WAF 제거 (Amplify로 정적 호스팅, API Gateway로 API)"
```

---

## Task 2: Terraform — Secrets Manager 제거

**Files:**
- Delete: `infra/terraform/secrets.tf`

- [ ] **Step 1: 파일 삭제 + 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm infra/terraform/secrets.tf
cd infra/terraform
terraform fmt -check && terraform validate
```
Expected: `Success! The configuration is valid.`

`outputs.tf`나 다른 파일이 `aws_secretsmanager_secret.*`를 참조하면 그 줄도 삭제. ESO IRSA의 정책(`eso_secrets_manager`)은 다음 Task 3에서 제거하므로 여기서는 건드리지 않음.

- [ ] **Step 2: 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git add -A infra/terraform/
git commit -m "[refactor] terraform: Secrets Manager 리소스 제거 (Sealed Secrets로 교체)"
```

---

## Task 3: Terraform — External Secrets Operator(ESO) 제거

**Files:**
- Modify: `infra/terraform/eks.tf` (eso_irsa 모듈 + helm_release "external_secrets" 제거)
- Modify: `infra/terraform/iam.tf` (`aws_iam_role_policy "eso_secrets_manager"` 제거)

- [ ] **Step 1: eks.tf에서 ESO 관련 블록 삭제**

`infra/terraform/eks.tf`에서 아래 3개 블록을 모두 삭제:

```hcl
module "eso_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.39"
  role_name = "${var.project}-external-secrets"
  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["external-secrets:external-secrets"]
    }
  }
}
```

```hcl
resource "aws_iam_role_policy" "eso_secrets_manager" {
  name = "${var.project}-eso-secrets-manager"
  role = module.eso_irsa.iam_role_name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
      Resource = "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.project}/*"
    }]
  })
}
```

```hcl
resource "helm_release" "external_secrets" {
  name             = "external-secrets"
  repository       = "https://charts.external-secrets.io"
  chart            = "external-secrets"
  namespace        = "external-secrets"
  create_namespace = true
  version          = "0.9.17"
  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = module.eso_irsa.iam_role_arn
  }
  depends_on = [module.eks]
}
```

또한 `helm_release "argocd_bootstrap"`의 `depends_on` 리스트에서 `helm_release.external_secrets`가 있으면 제거(있다면 Task 4 이후 검증 단계에서 잡힐 것).

- [ ] **Step 2: iam.tf 확인**

ESO 관련 정책은 위에서 이미 제거. iam.tf에는 ESO 관련 추가 코드가 보통 없음(있으면 함께 제거).

- [ ] **Step 3: 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
cd /c/Users/woomin/Desktop/Project/dev
! grep -nE "eso_irsa|external_secrets|eso_secrets_manager" infra/terraform/eks.tf infra/terraform/iam.tf && echo "ESO REMOVED OK"
```
Expected: `Success! The configuration is valid.` + `ESO REMOVED OK`

- [ ] **Step 4: 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git add infra/terraform/eks.tf infra/terraform/iam.tf
git commit -m "[refactor] terraform: External Secrets Operator 및 IRSA 제거"
```

---

## Task 4: Terraform — Fluent Bit / CloudWatch 알람·대시보드 제거

**Files:**
- Modify: `infra/terraform/eks.tf` (fluentbit_irsa, helm_release "aws_for_fluent_bit" 제거)
- Delete: `infra/terraform/monitoring.tf` (CloudWatch alarms/dashboard/log groups)

- [ ] **Step 1: eks.tf의 Fluent Bit 섹션 삭제**

`# ── Fluent Bit: 컨테이너 로그 → CloudWatch ───…` 주석 라인부터 그 아래 `module "fluentbit_irsa"`, `aws_iam_role_policy` (있다면), `helm_release "aws_for_fluent_bit"`까지 모두 삭제. 정확한 라인은 `grep -n fluentbit infra/terraform/eks.tf`로 위치 확인.

- [ ] **Step 2: monitoring.tf 통째로 삭제**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm infra/terraform/monitoring.tf
```

- [ ] **Step 3: 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
cd /c/Users/woomin/Desktop/Project/dev
! grep -nE "fluentbit|fluent_bit|aws_cloudwatch" infra/terraform/eks.tf && echo "FLUENTBIT REMOVED OK"
test ! -f infra/terraform/monitoring.tf && echo "MONITORING TF DELETED"
```
Expected: `Success! The configuration is valid.` + 두 OK.

- [ ] **Step 4: 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git add -A infra/terraform/
git commit -m "[refactor] terraform: Fluent Bit 및 CloudWatch 알람/대시보드 제거 (Prometheus만 사용)"
```

---

## Task 5: Terraform — us-east-1 ACM 인증서 제거

**Files:**
- Modify: `infra/terraform/acm.tf`

- [ ] **Step 1: acm.tf에서 cloudfront 인증서 블록 제거**

다음 2개 리소스를 삭제(ap-northeast-2 인증서·검증·Route53 검증 레코드는 유지):

```hcl
resource "aws_acm_certificate" "cloudfront" {
  provider                  = aws.us_east_1
  domain_name               = var.domain_name
  subject_alternative_names = ["*.${var.domain_name}"]
  validation_method         = "DNS"
  lifecycle { create_before_destroy = true }
}

resource "aws_acm_certificate_validation" "cloudfront" {
  provider                = aws.us_east_1
  certificate_arn         = aws_acm_certificate.cloudfront.arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}
```

`main.tf`의 `provider "aws" { alias = "us_east_1" }`는 일단 유지(다른 곳에서 안 쓰면 추후 정리).

- [ ] **Step 2: 검증 + 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
cd /c/Users/woomin/Desktop/Project/dev
! grep -q "aws_acm_certificate\.cloudfront" infra/terraform/acm.tf && echo "US-EAST-1 ACM REMOVED"
git add infra/terraform/acm.tf
git commit -m "[refactor] terraform: CloudFront용 us-east-1 ACM 인증서 제거"
```

---

## Task 6: Terraform — ECR frontend repo 제거

**Files:**
- Modify: `infra/terraform/ecr.tf`

- [ ] **Step 1: ecr.tf에서 frontend repo 삭제**

다음 2개 리소스 삭제(backend repo와 backend lifecycle policy는 유지):
```hcl
resource "aws_ecr_repository" "frontend" {
  name                 = "${var.project}/frontend"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

resource "aws_ecr_lifecycle_policy" "frontend" {
  repository = aws_ecr_repository.frontend.name
  policy     = aws_ecr_lifecycle_policy.backend.policy
}
```

`outputs.tf`에 `ecr_frontend_url` 출력이 있으면 함께 제거.

- [ ] **Step 2: 검증 + 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
cd /c/Users/woomin/Desktop/Project/dev
! grep -q "aws_ecr_repository\" \"frontend\"" infra/terraform/ecr.tf && echo "ECR FRONTEND REMOVED"
git add infra/terraform/ecr.tf infra/terraform/outputs.tf
git commit -m "[refactor] terraform: ECR frontend repo 제거 (Amplify 빌드/배포)"
```

---

## Task 7: Kustomize base — ExternalSecret 디렉토리 제거

**Files:**
- Delete: `k8s/base/external-secrets/` (디렉토리 전체)
- Modify: `k8s/base/kustomization.yaml`

- [ ] **Step 1: 디렉토리 삭제 + base kustomization에서 항목 제거**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm -rf k8s/base/external-secrets
```

`k8s/base/kustomization.yaml`에서 라인 삭제: `  - external-secrets/backend-secrets.yaml`

- [ ] **Step 2: base 렌더 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
kubectl kustomize k8s/base > /tmp/base.yaml && \
! grep -q "kind: ExternalSecret" /tmp/base.yaml && \
echo "BASE NO ESO OK"
```
Expected: `BASE NO ESO OK`

- [ ] **Step 3: 커밋**

```bash
git add -A k8s/base
git commit -m "[refactor] k8s base: ExternalSecret 리소스 제거 (Sealed Secrets로 교체)"
```

---

## Task 8: Kustomize base — frontend 워크로드 제거

**Files:**
- Delete: `k8s/base/frontend/` (디렉토리 전체)
- Modify: `k8s/base/kustomization.yaml`

- [ ] **Step 1: 디렉토리 삭제 + kustomization 항목 제거**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm -rf k8s/base/frontend
```

`k8s/base/kustomization.yaml`에서 다음 4개 라인 삭제:
```
  - frontend/deployment.yaml
  - frontend/service.yaml
  - frontend/hpa.yaml
  - frontend/pdb.yaml
```

- [ ] **Step 2: 렌더 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
kubectl kustomize k8s/base > /tmp/base.yaml && \
! grep -q "name: frontend" /tmp/base.yaml && \
echo "BASE NO FRONTEND OK"
```
Expected: `BASE NO FRONTEND OK`

- [ ] **Step 3: 커밋**

```bash
git add -A k8s/base
git commit -m "[refactor] k8s base: frontend 워크로드 제거 (Amplify로 호스팅)"
```

---

## Task 9: Kustomize base — Ingress를 internal/백엔드 단독으로 변경

**Files:**
- Modify: `k8s/base/ingress.yaml`

- [ ] **Step 1: ingress.yaml을 backend 전용 + internal로 교체**

`k8s/base/ingress.yaml` 전체를 아래 내용으로 교체:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: main
  namespace: student-mgmt
  annotations:
    argocd.argoproj.io/sync-wave: "2"
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internal
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP":80}]'
    alb.ingress.kubernetes.io/healthcheck-path: /api/health
spec:
  ingressClassName: alb
  rules:
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: backend
            port:
              number: 8080
```

핵심 변경: `scheme: internal`(인터넷 비노출), `listen-ports`에서 HTTPS 제거(API Gateway가 TLS 종료), 모든 경로(`/`)를 backend로(API Gateway 뒤에서는 경로 분기 불필요), `/swagger-ui`·`/v3/api-docs` 분기 제거(backend가 모두 처리), frontend 라우팅 제거.

- [ ] **Step 2: 렌더 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
kubectl kustomize k8s/base | grep -E "scheme|listen-ports|name: (frontend|backend)" 
```
Expected: `scheme: internal`, `[{"HTTP":80}]`만, frontend 미등장.

- [ ] **Step 3: 커밋**

```bash
git add k8s/base/ingress.yaml
git commit -m "[refactor] k8s base ingress: scheme=internal, backend 단독 라우팅"
```

---

## Task 10: Kustomize base — NetworkPolicy에서 frontend 제거

**Files:**
- Modify: `k8s/base/network-policy.yaml`

- [ ] **Step 1: network-policy.yaml을 backend 전용으로 교체**

`k8s/base/network-policy.yaml` 전체를 아래 내용으로 교체:

```yaml
# 백엔드: ALB(VPC CIDR) 헬스체크·트래픽 허용
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-policy
  namespace: student-mgmt
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
  - Ingress
  ingress:
  - from:
    - ipBlock:
        cidr: 10.0.0.0/16
    ports:
    - protocol: TCP
      port: 8080
```

frontend 정책과 backend 정책의 `podSelector: app: frontend` 소스 모두 삭제.

- [ ] **Step 2: 렌더 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
kubectl kustomize k8s/base | grep -c "kind: NetworkPolicy"
```
Expected: `1`

- [ ] **Step 3: 커밋**

```bash
git add k8s/base/network-policy.yaml
git commit -m "[refactor] k8s base network-policy: frontend 정책 및 frontend 소스 제거"
```

---

## Task 11: Kustomize overlays — frontend image + externalsecret-patch 제거, sealed-secrets 디렉토리 준비

**Files:**
- Delete: `k8s/overlays/{dev,staging,prod}/externalsecret-patch.yaml`
- Modify: `k8s/overlays/{dev,staging,prod}/kustomization.yaml`
- Modify: `k8s/overlays/{dev,staging,prod}/ingress-patch.yaml`

`<env>`를 `dev`, `staging`, `prod` 각각으로 치환해 3번 반복(아래는 dev 예시).

- [ ] **Step 1: externalsecret-patch.yaml 삭제 (3개 환경 모두)**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm k8s/overlays/dev/externalsecret-patch.yaml \
       k8s/overlays/staging/externalsecret-patch.yaml \
       k8s/overlays/prod/externalsecret-patch.yaml
```

- [ ] **Step 2: dev kustomization.yaml 교체**

`k8s/overlays/dev/kustomization.yaml` 전체 교체:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: student-mgmt-dev
resources:
  - ../../base
  - sealed-secrets  # 컷오버 단계에서 운영자가 SealedSecret YAML을 이 디렉토리에 커밋
images:
  - name: student-mgmt/backend
    newName: <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/student-mgmt/backend
    newTag: latest
patches:
  - path: ingress-patch.yaml
```

`images:` 블록에서 frontend 항목 제거, `patches:`에서 `externalsecret-patch.yaml` 제거, `resources:`에 `sealed-secrets` 디렉토리 추가.

- [ ] **Step 3: dev ingress-patch.yaml 간소화**

`k8s/overlays/dev/ingress-patch.yaml` 전체 교체:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: main
  namespace: student-mgmt
  annotations:
    alb.ingress.kubernetes.io/group.name: student-mgmt-dev
```

ACM 인증서·external-dns hostname 어노테이션은 제거(internal ALB는 TLS 없이 HTTP, DNS는 API Gateway가 처리). `group.name`만 환경 식별용으로 유지.

- [ ] **Step 4: dev sealed-secrets 디렉토리에 placeholder kustomization 작성**

`k8s/overlays/dev/sealed-secrets/kustomization.yaml` 생성:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources: []
# 컷오버 시 운영자가 kubeseal로 생성한 SealedSecret YAML 파일들을
# resources 리스트에 추가하고 커밋한다.
# 예: resources: [backend-secrets.yaml]
```

- [ ] **Step 5: staging/prod도 동일 구조 적용 (값만 치환)**

`<env>`를 staging/prod로 바꿔 Step 2~4를 반복:
- kustomization.yaml: `namespace: student-mgmt-<env>`, `group.name: student-mgmt-<env>`
- ingress-patch.yaml: `group.name: student-mgmt-<env>`
- sealed-secrets/kustomization.yaml: 동일 placeholder

- [ ] **Step 6: 렌더 검증 (3 환경)**

```bash
cd /c/Users/woomin/Desktop/Project/dev
for env in dev staging prod; do
  kubectl kustomize k8s/overlays/$env > /tmp/$env.yaml || { echo "$env BUILD FAIL"; exit 1; }
  grep -q "namespace: student-mgmt-$env" /tmp/$env.yaml || { echo "$env NS FAIL"; exit 1; }
  ! grep -q "name: frontend" /tmp/$env.yaml || { echo "$env FRONTEND LEAKED"; exit 1; }
  ! grep -q "kind: ExternalSecret" /tmp/$env.yaml || { echo "$env ESO LEAKED"; exit 1; }
  grep -q "scheme: internal" /tmp/$env.yaml || { echo "$env SCHEME FAIL"; exit 1; }
  echo "$env OK"
done
```
Expected: 세 줄 `dev OK`, `staging OK`, `prod OK`.

- [ ] **Step 7: 커밋**

```bash
git add -A k8s/overlays
git commit -m "[refactor] k8s overlays: frontend/ESO 제거, sealed-secrets 디렉토리 준비"
```

---

## Task 12: Bootstrap chart — ESO 템플릿 제거, AppProject/ApplicationSet만 유지

**Files:**
- Delete: `k8s/bootstrap/templates/clustersecretstore.yaml`
- Delete: `k8s/bootstrap/templates/argocd-admin-externalsecret.yaml`
- Delete: `k8s/bootstrap/templates/argocd-repo-externalsecret.yaml`
- Modify: `k8s/bootstrap/values.yaml` (region 항목 제거)

- [ ] **Step 1: 3개 템플릿 삭제**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git rm k8s/bootstrap/templates/clustersecretstore.yaml \
       k8s/bootstrap/templates/argocd-admin-externalsecret.yaml \
       k8s/bootstrap/templates/argocd-repo-externalsecret.yaml
```

- [ ] **Step 2: values.yaml 정리**

`k8s/bootstrap/values.yaml` 전체 교체:

```yaml
repoURL: ""        # Terraform이 주입: https://github.com/<org>/<repo>.git
targetRevision: main
environments:
  - dev
  - staging
  - prod
```

`region` 필드 제거(ClusterSecretStore 없어졌으니 불필요).

- [ ] **Step 3: 차트 렌더 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
helm lint k8s/bootstrap
helm template b k8s/bootstrap --set repoURL=https://github.com/example/dev.git > /tmp/boot.yaml && \
grep -q "kind: ApplicationSet" /tmp/boot.yaml && \
grep -q "kind: AppProject" /tmp/boot.yaml && \
! grep -q "kind: ClusterSecretStore" /tmp/boot.yaml && \
! grep -q "kind: ExternalSecret" /tmp/boot.yaml && \
echo "BOOTSTRAP MINIMAL OK"
```
Expected: lint 0 failures, `BOOTSTRAP MINIMAL OK`.

- [ ] **Step 4: 커밋**

```bash
git add -A k8s/bootstrap
git commit -m "[refactor] bootstrap chart: ESO 관련 템플릿 제거, AppProject/ApplicationSet만 유지"
```

---

## Task 13: cd.yml — frontend 빌드/푸시 제거 + paths-ignore 갱신

**Files:**
- Modify: `.github/workflows/cd.yml`

- [ ] **Step 1: paths-ignore에 frontend/** 추가**

`.github/workflows/cd.yml`의 `on.push.paths-ignore` 블록에 `'frontend/**'`를 추가:

```yaml
on:
  push:
    branches: [main]
    tags:
      - 'v*.*.*'
    paths-ignore:
      - 'k8s/**'
      - 'docs/**'
      - 'frontend/**'
```

- [ ] **Step 2: frontend 빌드/푸시 스텝 제거**

`.github/workflows/cd.yml`에서 다음 스텝 전체 삭제(backend 빌드/푸시 스텝은 유지):

```yaml
      - name: Build & push frontend
        uses: docker/build-push-action@v5
        with:
          context: frontend
          file: frontend/Dockerfile
          push: true
          tags: |
            ${{ steps.ecr_login.outputs.registry }}/student-mgmt/frontend:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

- [ ] **Step 3: write-back 스텝의 frontend 라인 제거**

`Write-back image tag to dev overlay` 스텝의 `kustomize edit set image` 명령에서 frontend 줄 삭제:

```yaml
      - name: Write-back image tag to dev overlay
        if: github.ref == 'refs/heads/main'
        run: |
          cd k8s/overlays/dev
          kustomize edit set image \
            student-mgmt/backend=${{ steps.ecr_login.outputs.registry }}/student-mgmt/backend:${{ github.sha }}
```

- [ ] **Step 4: YAML 검증 + 잔재 점검**

```bash
cd /c/Users/woomin/Desktop/Project/dev
PYTHONUTF8=1 python -c "import yaml; yaml.safe_load(open('.github/workflows/cd.yml', encoding='utf-8')); print('YAML OK')"
! grep -q "student-mgmt/frontend" .github/workflows/cd.yml && \
grep -q "'frontend/\*\*'" .github/workflows/cd.yml && \
echo "CD FRONTEND REMOVED + PATHS-IGNORE OK"
```
Expected: `YAML OK` + `CD FRONTEND REMOVED + PATHS-IGNORE OK`

- [ ] **Step 5: 커밋**

```bash
git add .github/workflows/cd.yml
git commit -m "[refactor] cd.yml: frontend 빌드/푸시/write-back 제거 + paths-ignore에 frontend/** 추가"
```

---

## Task 14: Terraform — Sealed Secrets controller helm_release 추가

**Files:**
- Modify: `infra/terraform/eks.tf` (helm_release "sealed_secrets" 추가)
- Modify: `infra/terraform/variables.tf` (`sealed_secrets_chart_version` 추가)

- [ ] **Step 1: variables.tf에 차트 버전 변수 추가**

`infra/terraform/variables.tf` 끝에 추가:

```hcl
variable "sealed_secrets_chart_version" {
  description = "sealed-secrets helm 차트 버전"
  type        = string
  default     = "2.16.1"
}
```

- [ ] **Step 2: eks.tf 끝에 helm_release 추가**

`infra/terraform/eks.tf`의 Argo CD bootstrap 위쪽 또는 맨 끝에 추가:

```hcl
# ── Sealed Secrets ─────────────────────────────────────────────────────────

resource "helm_release" "sealed_secrets" {
  name             = "sealed-secrets"
  repository       = "https://bitnami-labs.github.io/sealed-secrets"
  chart            = "sealed-secrets"
  namespace        = "sealed-secrets"
  create_namespace = true
  version          = var.sealed_secrets_chart_version

  depends_on = [module.eks]
}
```

- [ ] **Step 3: 검증 + 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
cd /c/Users/woomin/Desktop/Project/dev
grep -q "helm_release\" \"sealed_secrets\"" infra/terraform/eks.tf && echo "SEALED SECRETS HR PRESENT"
git add infra/terraform/eks.tf infra/terraform/variables.tf
git commit -m "[feat] terraform: Sealed Secrets controller helm_release 추가"
```

---

## Task 15: Terraform — Amplify (앱 + 브랜치 × 3 + 도메인)

**Files:**
- Create: `infra/terraform/amplify.tf`
- Modify: `infra/terraform/variables.tf` (`amplify_github_token` 추가)
- Modify: `infra/terraform/outputs.tf` (Amplify 기본 도메인 출력)

- [ ] **Step 1: variables.tf에 GitHub PAT 변수 추가**

`infra/terraform/variables.tf` 끝에 추가:

```hcl
variable "amplify_github_token" {
  description = "Amplify가 GitHub repo를 watch하기 위한 PAT (repo, admin:repo_hook 권한)"
  type        = string
  sensitive   = true
}
```

- [ ] **Step 2: amplify.tf 작성**

`infra/terraform/amplify.tf`:

```hcl
# ── AWS Amplify (React SPA) ─────────────────────────────────────────────────

resource "aws_amplify_app" "main" {
  name        = var.project
  repository  = "https://github.com/${var.github_org}/${var.github_repo}"
  access_token = var.amplify_github_token
  platform    = "WEB"

  build_spec = <<-EOT
    version: 1
    applications:
      - appRoot: frontend
        frontend:
          phases:
            preBuild:
              commands:
                - npm install
            build:
              commands:
                - npm run build
          artifacts:
            baseDirectory: dist
            files:
              - '**/*'
          cache:
            paths:
              - node_modules/**/*
  EOT

  enable_branch_auto_build       = true
  enable_branch_auto_deletion    = false

  custom_rule {
    source = "/<*>"
    target = "/index.html"
    status = "404-200"
  }
}

locals {
  amplify_branches = {
    dev     = { branch = "develop", env = "dev", prefix = "dev" }
    staging = { branch = "staging", env = "staging", prefix = "staging" }
    prod    = { branch = "main", env = "prod", prefix = "" }
  }
}

resource "aws_amplify_branch" "env" {
  for_each = local.amplify_branches

  app_id      = aws_amplify_app.main.id
  branch_name = each.value.branch
  framework   = "React"
  stage       = each.key == "prod" ? "PRODUCTION" : "DEVELOPMENT"

  enable_auto_build = true

  environment_variables = {
    VITE_API_URL = each.value.prefix == "" ? "https://api.${var.domain_name}" : "https://api-${each.value.prefix}.${var.domain_name}"
  }
}

resource "aws_amplify_domain_association" "main" {
  app_id      = aws_amplify_app.main.id
  domain_name = var.domain_name

  dynamic "sub_domain" {
    for_each = local.amplify_branches
    content {
      branch_name = aws_amplify_branch.env[sub_domain.key].branch_name
      prefix      = sub_domain.value.prefix
    }
  }
}
```

- [ ] **Step 3: outputs.tf에 amplify 출력 추가**

`infra/terraform/outputs.tf` 끝에 추가:

```hcl
output "amplify_app_id" {
  description = "Amplify 앱 ID (콘솔 링크용)"
  value       = aws_amplify_app.main.id
}

output "amplify_default_domain" {
  description = "Amplify 기본 amplifyapp.com 도메인"
  value       = aws_amplify_app.main.default_domain
}
```

- [ ] **Step 4: 검증 + 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
cd /c/Users/woomin/Desktop/Project/dev
grep -q "aws_amplify_app" infra/terraform/amplify.tf && \
grep -q "amplify_github_token" infra/terraform/variables.tf && \
echo "AMPLIFY TF OK"
git add infra/terraform/amplify.tf infra/terraform/variables.tf infra/terraform/outputs.tf
git commit -m "[feat] terraform: Amplify 앱 + 브랜치 3개 + 도메인 연결"
```

---

## Task 16: Terraform — API Gateway × 3 (HTTP API + VPC Link + 커스텀 도메인)

**Files:**
- Create: `infra/terraform/apigateway.tf`
- Modify: `infra/terraform/outputs.tf` (API Gateway endpoint 출력)

> ⚠️ 이 Task의 코드는 ALB가 이미 존재해야 apply 가능(data source가 ALB 조회). 첫 `terraform apply` 시점에는 ALB가 없으므로 `terraform plan/validate`까지만 본 Task 범위. 실제 apply는 Task 21 컷오버에서 2-phase로 진행(EKS+Argo apply → Argo가 Ingress 생성 → ALB 생성 → 두 번째 apply에서 API Gateway).

- [ ] **Step 1: apigateway.tf 작성**

`infra/terraform/apigateway.tf`:

```hcl
# ── API Gateway HTTP API × 3 (환경별 별도) ─────────────────────────────────

locals {
  apigw_envs = {
    dev     = { prefix = "api-dev", ns = "student-mgmt-dev" }
    staging = { prefix = "api-staging", ns = "student-mgmt-staging" }
    prod    = { prefix = "api", ns = "student-mgmt-prod" }
  }
}

# 환경별 private ALB 조회 (aws-load-balancer-controller가 만든 ALB의 태그로 식별)
# Argo가 Ingress를 sync한 뒤에야 ALB가 존재 → 본 data source는 2-phase apply의 두 번째에서만 resolve
data "aws_lb" "env" {
  for_each = local.apigw_envs

  tags = {
    "ingress.k8s.aws/stack"  = "${each.value.ns}/main"
    "elbv2.k8s.aws/cluster"  = module.eks.cluster_name
  }
}

data "aws_lb_listener" "env_http" {
  for_each          = data.aws_lb.env
  load_balancer_arn = each.value.arn
  port              = 80
}

# VPC Link (환경별)
resource "aws_security_group" "vpc_link" {
  for_each = local.apigw_envs

  name        = "${var.project}-vpclink-${each.key}-sg"
  description = "API Gateway VPC Link → ALB (${each.key})"
  vpc_id      = module.vpc.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_apigatewayv2_vpc_link" "env" {
  for_each = local.apigw_envs

  name               = "${var.project}-${each.key}"
  security_group_ids = [aws_security_group.vpc_link[each.key].id]
  subnet_ids         = module.vpc.private_subnets
}

# HTTP API
resource "aws_apigatewayv2_api" "env" {
  for_each = local.apigw_envs

  name          = "${var.project}-${each.key}"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = each.key == "prod" ? ["https://${var.domain_name}"] : ["https://${each.value.prefix == "api" ? "" : replace(each.value.prefix, "api-", "")}.${var.domain_name}"]
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["Authorization", "Content-Type"]
    max_age       = 600
  }
}

resource "aws_apigatewayv2_integration" "env" {
  for_each = local.apigw_envs

  api_id             = aws_apigatewayv2_api.env[each.key].id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = data.aws_lb_listener.env_http[each.key].arn
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.env[each.key].id

  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "env_proxy" {
  for_each = local.apigw_envs

  api_id    = aws_apigatewayv2_api.env[each.key].id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.env[each.key].id}"
}

resource "aws_apigatewayv2_stage" "env_default" {
  for_each = local.apigw_envs

  api_id      = aws_apigatewayv2_api.env[each.key].id
  name        = "$default"
  auto_deploy = true
}

# 커스텀 도메인 (api.<domain> / api-dev.<domain> / api-staging.<domain>)
resource "aws_apigatewayv2_domain_name" "env" {
  for_each = local.apigw_envs

  domain_name = "${each.value.prefix}.${var.domain_name}"

  domain_name_configuration {
    certificate_arn = aws_acm_certificate_validation.main.certificate_arn
    endpoint_type   = "REGIONAL"
    security_policy = "TLS_1_2"
  }
}

resource "aws_apigatewayv2_api_mapping" "env" {
  for_each = local.apigw_envs

  api_id      = aws_apigatewayv2_api.env[each.key].id
  domain_name = aws_apigatewayv2_domain_name.env[each.key].id
  stage       = aws_apigatewayv2_stage.env_default[each.key].name
}

resource "aws_route53_record" "apigw_env" {
  for_each = local.apigw_envs

  zone_id = data.aws_route53_zone.main.zone_id
  name    = aws_apigatewayv2_domain_name.env[each.key].domain_name
  type    = "A"

  alias {
    name                   = aws_apigatewayv2_domain_name.env[each.key].domain_name_configuration[0].target_domain_name
    zone_id                = aws_apigatewayv2_domain_name.env[each.key].domain_name_configuration[0].hosted_zone_id
    evaluate_target_health = false
  }
}

# ALB가 VPC Link로부터 트래픽 받게 허용 (EKS 노드 SG는 ALB SG로 이미 허용됨)
# aws-load-balancer-controller가 만든 ALB의 SG에 VPC Link SG 허용 추가
data "aws_security_group" "alb_env" {
  for_each = data.aws_lb.env
  filter {
    name   = "tag:elbv2.k8s.aws/cluster"
    values = [module.eks.cluster_name]
  }
  filter {
    name   = "tag:ingress.k8s.aws/stack"
    values = ["${local.apigw_envs[each.key].ns}/main"]
  }
}

resource "aws_security_group_rule" "alb_from_vpclink" {
  for_each = local.apigw_envs

  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.vpc_link[each.key].id
  security_group_id        = data.aws_security_group.alb_env[each.key].id
}
```

- [ ] **Step 2: outputs.tf에 API Gateway endpoint 출력 추가**

```hcl
output "apigateway_endpoints" {
  description = "환경별 API Gateway invoke URL"
  value = {
    for k, v in aws_apigatewayv2_api.env : k => "https://${aws_apigatewayv2_domain_name.env[k].domain_name}"
  }
}
```

- [ ] **Step 3: 검증 (validate만; apply는 컷오버)**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform fmt -check && terraform validate
```
Expected: `Success! The configuration is valid.`

(validate는 data source의 실제 존재를 확인하지 않으므로 ALB 없어도 통과)

- [ ] **Step 4: 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
git add infra/terraform/apigateway.tf infra/terraform/outputs.tf
git commit -m "[feat] terraform: API Gateway HTTP API 3개 + VPC Link + 커스텀 도메인 (환경별)"
```

---

## Task 17: amplify.yml (repo 루트)

**Files:**
- Create: `amplify.yml`

> 이 파일은 Amplify Console이 frontend/ 디렉토리에 적용할 빌드 스펙. Terraform amplify_app build_spec은 인라인으로 동일 내용을 가지므로 둘 중 하나만 있어도 동작하지만, repo에 두면 가시성 좋음.

- [ ] **Step 1: amplify.yml 작성**

`amplify.yml`:

```yaml
version: 1
applications:
  - appRoot: frontend
    frontend:
      phases:
        preBuild:
          commands:
            - npm install
        build:
          commands:
            - npm run build
      artifacts:
        baseDirectory: dist
        files:
          - '**/*'
      cache:
        paths:
          - node_modules/**/*
```

- [ ] **Step 2: 검증 + 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
PYTHONUTF8=1 python -c "import yaml; yaml.safe_load(open('amplify.yml', encoding='utf-8')); print('AMPLIFY YML OK')"
git add amplify.yml
git commit -m "[feat] amplify.yml: frontend 모노레포 빌드 스펙"
```

---

## Task 18: 전체 정적 검증 게이트

**Files:** 없음 (검증만)

- [ ] **Step 1: 모든 검증 일괄 실행**

```bash
cd /c/Users/woomin/Desktop/Project/dev
set -e
echo "=== kustomize overlays ==="
for env in dev staging prod; do kubectl kustomize k8s/overlays/$env >/dev/null && echo "kustomize $env OK"; done
echo "=== helm bootstrap ==="
helm lint k8s/bootstrap
helm template b k8s/bootstrap --set repoURL=https://github.com/example/dev.git >/dev/null && echo "helm OK"
echo "=== workflows YAML ==="
for f in .github/workflows/cd.yml .github/workflows/promote.yml .github/workflows/ci.yml amplify.yml; do
  PYTHONUTF8=1 python -c "import yaml,sys; yaml.safe_load(open(sys.argv[1],encoding='utf-8'))" "$f" && echo "$f OK"
done
echo "=== terraform ==="
( cd infra/terraform && terraform fmt -check && terraform init -backend=false >/dev/null && terraform validate )
echo ""
echo "=== 부정 점검: 잔재 없음 ==="
! grep -rn "aws_secretsmanager_secret\|aws_cloudfront\|aws_wafv2\|external_secrets\|aws_for_fluent_bit" infra/terraform/ --include='*.tf' || (echo "TF 잔재 발견" && exit 1)
! grep -rn "kind: ExternalSecret\|kind: ClusterSecretStore" k8s/ || (echo "k8s 잔재 발견" && exit 1)
echo "ALL STATIC CHECKS PASSED"
```
Expected: 모든 라인 OK + `ALL STATIC CHECKS PASSED`.

---

## Task 19: 시스템 문서 재작성

**Files:**
- Modify: `docs/SYSTEM_ARCHITECTURE.md` (전면 재작성)
- Modify: `docs/INFRASTRUCTURE.md` (전면 재작성)

- [ ] **Step 1: SYSTEM_ARCHITECTURE.md 재작성**

`docs/SYSTEM_ARCHITECTURE.md` 전체를 다음 구조로 재작성:
1. 한 화면 토폴로지 (Amplify + API Gateway + private ALB + EKS + RDS 다이어그램, spec §4와 동일)
2. 환경 분리 매트릭스 (3-env 네임스페이스, 도메인, ALB SG, DB 스키마)
3. 컴포넌트별 책임 (Amplify, API Gateway, ALB, EKS 워크로드, RDS, Sealed Secrets, Prometheus)
4. 요청 흐름 (SPA 정적, API 호출 — 별도 흐름)
5. 배포 흐름 (Amplify 자동 + GitHub Actions backend + Argo)
6. 보안 경계 (spec §5.9 표를 그대로)
7. 가용성·확장 (RollingUpdate, HPA, PDB, Multi-AZ)
8. 부트스트랩 요지

이전 문서의 CloudFront/Secrets Manager 관련 내용은 완전히 제거.

- [ ] **Step 2: INFRASTRUCTURE.md 재작성**

`docs/INFRASTRUCTURE.md` 전체를 다음 구조로 재작성:
1. Terraform 레이아웃 (파일 목록·각 책임)
2. AWS 리소스 카탈로그
   - VPC (변경 없음)
   - EKS (변경 없음)
   - RDS (변경 없음)
   - ECR (backend만)
   - ACM (regional 한 장)
   - Route53 (Amplify·API Gateway·argocd subdomains)
   - Amplify (앱·브랜치·도메인 연결)
   - API Gateway HTTP API × 3 + VPC Link × 3
3. EKS 애드온 helm_release 표 (aws_lbc, cluster_autoscaler, metrics_server, external_dns, kube_prometheus_stack, sealed_secrets, argocd, argocd_bootstrap)
4. IAM 모델 (GitHub OIDC ECR-only + IRSA 표 — ESO·Fluent Bit IRSA 행 제거)
5. 시크릿 모델 (Sealed Secrets 흐름)
6. 환경 분리 매트릭스
7. Terraform 부트스트랩 순서 (2-phase: EKS+Argo+SealedSecrets → Argo가 Ingress sync로 ALB 생성 → API Gateway apply)
8. 변경·확장 체크리스트
9. 비용·운영 주의사항

- [ ] **Step 3: 검증**

```bash
cd /c/Users/woomin/Desktop/Project/dev
! grep -q "CloudFront\|Secrets Manager\|External Secrets Operator\|ExternalSecret\|Fluent Bit\|WAF" docs/SYSTEM_ARCHITECTURE.md docs/INFRASTRUCTURE.md && echo "DOCS CLEAN"
grep -q "Amplify\|API Gateway\|Sealed Secrets" docs/SYSTEM_ARCHITECTURE.md && \
grep -q "Amplify\|API Gateway\|Sealed Secrets" docs/INFRASTRUCTURE.md && echo "DOCS COVER NEW STACK"
```
Expected: `DOCS CLEAN` + `DOCS COVER NEW STACK`

- [ ] **Step 4: 커밋**

```bash
git add docs/SYSTEM_ARCHITECTURE.md docs/INFRASTRUCTURE.md
git commit -m "[docs] SYSTEM_ARCHITECTURE/INFRASTRUCTURE: Amplify + API Gateway + Sealed Secrets로 재작성"
```

---

## Task 20: CLAUDE.md / AGENTS.md CI/CD 섹션 재작성

**Files:**
- Modify: `CLAUDE.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 두 파일의 `## CI/CD` 섹션을 동일한 새 내용으로 교체**

```markdown
## CI/CD

- **CI** (`.github/workflows/ci.yml`): `main`/`develop` 브랜치 push/PR 시 MySQL 컨테이너로 테스트 → JAR 빌드.
- **백엔드 CD** (`.github/workflows/cd.yml`): `main` push 시(`k8s/**`·`docs/**`·`frontend/**` 제외) OIDC로 AWS 인증 → **ECR**에 backend 이미지 빌드/푸시(불변 태그 `github.sha`) → `kustomize edit set image`로 `k8s/overlays/dev`의 이미지 태그를 갱신·커밋(write-back). **CI에 클러스터 자격증명 없음.**
- **프론트엔드 CD**: **Amplify Console**이 GitHub webhook으로 자동 빌드/배포. branch=`develop`→`dev.<domain>`, `staging`→`staging.<domain>`, `main`→`<domain>`(루트). GitHub Actions 워크플로우 불필요.
- **GitOps 배포**: **Argo CD**(EKS `argocd` 네임스페이스)가 git을 watch하여 `k8s/overlays/{env}`를 각 `student-mgmt-{env}` 네임스페이스에 동기화. dev는 자동, staging/prod는 `promote.yml`(workflow_dispatch) PR 머지로 승격.
- **시크릿**: 운영자가 `kubeseal`로 암호화한 `SealedSecret`를 `k8s/overlays/{env}/sealed-secrets/`에 커밋 → Argo가 sync → 클러스터 내 sealed-secrets controller가 복호화하여 Secret 생성 → Pod `envFrom`.
- **진입 토폴로지**: 브라우저 → Amplify(SPA, public) / 브라우저 → API Gateway HTTP API(public, 환경별 3개) → VPC Link → private ALB → EKS backend.
- **인프라**: Terraform(`infra/terraform/`)으로 VPC·EKS·RDS·ECR·ACM·Route53·Amplify·API Gateway·Sealed Secrets controller를 관리. Kustomize(`k8s/base` + `k8s/overlays/{dev,staging,prod}`)로 Deployment·Service·HPA·PDB·Ingress·NetworkPolicy·SealedSecret·ServiceMonitor 구성.
```

- [ ] **Step 2: 상세 문서 링크는 그대로 유지(SYSTEM_ARCHITECTURE.md, INFRASTRUCTURE.md가 이미 갱신됨)**

- [ ] **Step 3: 검증 + 커밋**

```bash
cd /c/Users/woomin/Desktop/Project/dev
! grep -E "CloudFront|Secrets Manager|External Secrets|kubectl로 EKS 롤링" CLAUDE.md AGENTS.md && echo "DOCS STALE PHRASES GONE"
grep -q "Amplify Console" CLAUDE.md && grep -q "Amplify Console" AGENTS.md && echo "AMPLIFY MENTIONED"
git add CLAUDE.md AGENTS.md
git commit -m "[docs] CLAUDE.md/AGENTS.md CI/CD 섹션을 Amplify + API Gateway + Sealed Secrets 기준으로 재작성"
```

---

## Task 21: 라이브 컷오버 런북 (수동 — AWS 자격증명 필요)

> 이 Task는 코드 변경 0건. 운영자가 실제 AWS에 적용하는 절차. 본 계획의 검증은 정적 검증까지(`Task 18`). end-to-end 확인은 이 절차로.

**전제조건**:
- `terraform.tfvars`에 `domain_name`, `db_password`, `jwt_secret`, `github_org`, `github_repo`, `amplify_github_token` 채움.
- Route53 호스팅 존이 `<domain>`에 존재. Terraform 상태 S3 버킷 + DynamoDB lock 테이블 사전 생성.
- GitHub repo 설정: Actions 탭에서 "Allow GitHub Actions to create and approve pull requests" 활성. `main` 보호 규칙이 있다면 `github-actions[bot]` 허용 또는 보호 해제.
- 로컬에 `kubeseal` CLI 설치(`brew install kubeseal` 또는 GitHub Releases).

- [ ] **Step 1: Phase 1 apply — 인프라 + 클러스터 + 애드온 + Argo + Sealed Secrets (API Gateway 제외)**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform apply -target=module.eks   # 클러스터 먼저
terraform apply -target=helm_release.aws_lbc \
                -target=helm_release.cluster_autoscaler \
                -target=helm_release.metrics_server \
                -target=helm_release.external_dns \
                -target=helm_release.kube_prometheus_stack \
                -target=helm_release.sealed_secrets \
                -target=helm_release.argocd \
                -target=helm_release.argocd_bootstrap \
                -target=aws_amplify_app.main \
                -target=aws_amplify_branch.env \
                -target=aws_amplify_domain_association.main
```

이때 API Gateway 리소스는 `data "aws_lb"`가 ALB를 못 찾아 실패하므로 -target으로 제외.

- [ ] **Step 2: kubeconfig 갱신 + Argo 상태 확인**

```bash
aws eks update-kubeconfig --name student-mgmt-cluster --region ap-northeast-2
kubectl get nodes
kubectl get pods -n argocd
kubectl get applications -n argocd   # 3개 Application이 sync 중일 것. 시크릿 부재로 backend Pod는 ImagePullBackOff 또는 CreateContainerConfigError 가능.
```

- [ ] **Step 3: 환경별 시크릿 생성·암호화·커밋 (3 환경 모두)**

```bash
cd /c/Users/woomin/Desktop/Project/dev

# 강력한 랜덤 값 생성 예
DB_PW=$(openssl rand -base64 24)
JWT=$(openssl rand -base64 48)

# dev 예시 (staging/prod 동일 절차, 값과 디렉토리만 치환)
kubectl create secret generic backend-secrets -n student-mgmt-dev \
  --from-literal=DB_USERNAME=appuser \
  --from-literal=DB_PASSWORD="$DB_PW" \
  --from-literal=SPRING_DATASOURCE_URL="jdbc:mysql://<rds-endpoint>:3306/student_mgmt_dev?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true" \
  --from-literal=JWT_SECRET="$JWT" \
  --dry-run=client -o yaml | \
kubeseal --controller-namespace sealed-secrets --format yaml \
  > k8s/overlays/dev/sealed-secrets/backend-secrets.yaml

# kustomization.yaml의 resources에 추가
sed -i 's|resources: \[\]|resources:\n  - backend-secrets.yaml|' k8s/overlays/dev/sealed-secrets/kustomization.yaml

# staging, prod도 동일 — 값(특히 JDBC URL의 DB명)만 치환

git add k8s/overlays/{dev,staging,prod}/sealed-secrets/
git commit -m "[chore] 환경별 SealedSecret 추가 (컷오버)"
git push
```

- [ ] **Step 4: RDS 논리 DB 3개 생성 (1회 SQL)**

```bash
kubectl run mysql-init --rm -it --image=mysql:8 -n student-mgmt-dev --restart=Never -- \
  mysql -h <rds-endpoint> -u <admin> -p<pw> -e \
  "CREATE DATABASE IF NOT EXISTS student_mgmt_dev; CREATE DATABASE IF NOT EXISTS student_mgmt_staging; CREATE DATABASE IF NOT EXISTS student_mgmt_prod;"
```

- [ ] **Step 5: Argo가 SealedSecret을 sync → Secret materialize 확인 → backend Pod 기동 확인**

```bash
kubectl get sealedsecrets -A
kubectl get secrets -n student-mgmt-dev backend-secrets   # Owned by SealedSecret
kubectl get pods -n student-mgmt-dev   # backend Pod Running
kubectl get ingress -n student-mgmt-dev   # ALB 호스트네임 발급 확인
```

3개 환경 모두 동일 확인. ALB가 생성됐는지 AWS 콘솔에서 scheme=internal 확인.

- [ ] **Step 6: Phase 2 apply — API Gateway (이제 ALB data source resolve 가능)**

```bash
cd /c/Users/woomin/Desktop/Project/dev/infra/terraform
terraform apply   # 모든 나머지 + API Gateway × 3 생성
```

- [ ] **Step 7: Amplify 브랜치 빌드 확인 + 도메인 검증**

```bash
# Amplify 콘솔에서 3개 브랜치 빌드 상태 확인
# Amplify 도메인 연결 검증(ACM/Route53 자동 처리) 완료 후:
curl -I https://dev.<domain>
curl -I https://api-dev.<domain>/api/health
```

CloudFront 없이 Amplify가 직접 TLS 종료. SPA 로드 + API 호출(CORS 동작) 브라우저로 확인.

- [ ] **Step 8: Sealed Secrets 키 백업**

```bash
mkdir -p ~/secure-backup
kubectl get secret -n sealed-secrets -l sealedsecrets.bitnami.com/sealed-secrets-key \
  -o yaml > ~/secure-backup/sealed-secrets-key-$(date +%F).yaml
```

이 파일은 외부 안전 저장소(예: 로컬 암호화 USB, Bitwarden)로 옮김. 클러스터 재생성 시 복원하지 않으면 모든 SealedSecret을 다시 만들어야 함.

- [ ] **Step 9: e2e CI 한 사이클 확인**

backend 코드를 사소하게 수정해서 main에 push → cd.yml이 ECR push + write-back commit + Argo dev sync → dev에서 새 이미지 배포 확인. frontend 코드 수정해서 develop에 push → Amplify가 자동 빌드 → dev.<domain>에 반영.

- [ ] **Step 10: 승격 PR 동작 확인**

GitHub Actions 탭 → `Promote` workflow → from=dev, to=staging 실행 → PR 자동 생성 → 머지 → Argo staging sync 확인.

---

## Self-Review

**1. Spec 커버리지**:
- §4 토폴로지 → Task 9,11,16,17
- §5.1 도메인/TLS → Task 5,15,16
- §5.2 Amplify → Task 15,17
- §5.3 API Gateway × 3 → Task 16
- §5.4 EKS 워크로드 → Task 7-11
- §5.5 Sealed Secrets → Task 14,21
- §5.6 GitOps → Task 12,21
- §5.7 CI/CD → Task 13,21
- §5.8 관측 → Task 4 (Fluent Bit 제거 = Prometheus만), Task 19 (문서)
- §5.9 보안 경계 → Task 19 (문서)
- §6 파일별 변경 → Task 1-17 전체
- §7 마이그레이션 → Task 21
- §8 가정/범위외 → 본문에 명시(Loki/SSO/Cognito 제외)
- §9 검증 → Task 18 + Task 21

**2. Placeholder 점검**: TBD/TODO 없음. 환경 고유값(`<ACCOUNT_ID>`, `<domain>`, `<rds-endpoint>`)은 컷오버에서 채움.

**3. 타입/명명 일관성**:
- 이미지 이름 `student-mgmt/backend`: base deployment, overlay images, cd.yml, promote.yml에서 모두 동일.
- 네임스페이스 `student-mgmt-{env}`: overlays, ApplicationSet, AppProject, API Gateway local map 모두 동일.
- 시크릿 이름 `backend-secrets`: base deployment의 envFrom, SealedSecret target name 모두 동일.
- ALB 태그 `ingress.k8s.aws/stack=student-mgmt-{env}/main`, `elbv2.k8s.aws/cluster=...`: 일관.
- helm_release 이름 `sealed_secrets`, `argocd`, `argocd_bootstrap`: 일관.

**4. 추가 발견**: API Gateway data source가 ALB 의존이라 2-phase apply 필요한 점을 Task 16 경고 + Task 21 Step 1/6에서 명시. CORS 동적 표현식은 Amplify 도메인 패턴(`https://<prefix>.<domain>` 또는 루트)에 맞춰 처리.
