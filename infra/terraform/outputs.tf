output "kubeconfig_command" {
  description = "로컬 kubectl 설정 명령어"
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.aws_region}"
}

output "ecr_backend_url" {
  description = "백엔드 ECR URL (CD 파이프라인 참고용)"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_url" {
  description = "프론트엔드 ECR URL"
  value       = aws_ecr_repository.frontend.repository_url
}

output "rds_endpoint" {
  description = "RDS 엔드포인트 (Secrets Manager에 자동 저장됨)"
  value       = module.rds.db_instance_endpoint
  sensitive   = true
}

output "cloudfront_domain" {
  description = "CloudFront 배포 도메인"
  value       = aws_cloudfront_distribution.main.domain_name
}

output "github_actions_role_arn" {
  description = "GitHub Secrets > AWS_ROLE_ARN 에 입력"
  value       = aws_iam_role.github_actions.arn
}

output "acm_cert_arn" {
  description = "k8s/ingress.yaml annotation에 입력"
  value       = aws_acm_certificate_validation.main.certificate_arn
}

output "reports_bucket" {
  description = "리포트 파일 저장 S3 버킷명"
  value       = aws_s3_bucket.reports.bucket
}
