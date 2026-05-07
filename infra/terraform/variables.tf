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
