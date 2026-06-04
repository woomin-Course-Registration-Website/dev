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

variable "domain_name" {
  description = "서비스 도메인 (e.g. example.com) — Route53에 호스팅 존 사전 등록 필요"
  type        = string
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

variable "amplify_github_token" {
  description = "Amplify가 GitHub repo를 watch하기 위한 PAT (repo, admin:repo_hook 권한)"
  type        = string
  sensitive   = true
}
