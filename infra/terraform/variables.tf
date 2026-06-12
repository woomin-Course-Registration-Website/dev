variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "project" {
  type    = string
  default = "student-mgmt"
}

variable "eks_node_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "EKS 퍼블릭 API 엔드포인트 접근 허용 CIDR. 운영 시 사무실/VPN IP로 좁히는 것을 권장."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "eks_node_desired" {
  type    = number
  default = 2
}

variable "eks_node_min" {
  description = "무중단 배포를 위해 최소 2 유지"
  type        = number
  default     = 2
}

variable "eks_node_max" {
  type    = number
  default = 5
}

variable "db_username" {
  type      = string
  default   = "appuser"
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "jwt_secret" {
  description = "JWT 서명 키 (256비트 이상)"
  type        = string
  sensitive   = true
}

variable "github_org" {
  description = "GitHub 사용자명 또는 Organization명 (OIDC 신뢰 정책용)"
  type        = string
}

variable "github_repo" {
  type    = string
  default = "dev"
}

variable "argocd_chart_version" {
  description = "argo-cd helm 차트 버전"
  type        = string
  default     = "7.7.7"
}

variable "sealed_secrets_chart_version" {
  description = "sealed-secrets helm 차트 버전"
  type        = string
  default     = "2.16.1"
}

variable "strimzi_chart_version" {
  description = "strimzi-kafka-operator helm 차트 버전 (학습 분석 CDC — 가점)"
  type        = string
  default     = "0.45.0"
}

variable "amplify_github_token" {
  description = "Amplify가 GitHub repo를 watch하기 위한 PAT (repo, admin:repo_hook 권한)"
  type        = string
  sensitive   = true
}

# 실제로 public 진입점(API Gateway)을 프로비저닝할 환경.
# dev만 ALB가 떠 있으므로 기본 ["dev"]. staging/prod는 promote.yml로 승격되어
# 백엔드가 Healthy → Ingress ALB 생성된 뒤 이 목록에 추가한다.
variable "live_envs" {
  description = "API Gateway를 생성할 환경 목록 (ALB가 존재해야 함)"
  type        = list(string)
  default     = ["dev"]
}

# API Gateway CORS 허용 Origin.
# Amplify가 콘솔 GitHub App으로 연결되므로(org deploy-key 제약) Terraform이
# Amplify 도메인을 알 수 없음 → 우선 "*"로 두고, Amplify URL 확정 후 좁힌다.
variable "frontend_origins" {
  description = "API Gateway CORS allow_origins (Amplify URL 확정 후 좁힘)"
  type        = list(string)
  default     = ["*"]
}
