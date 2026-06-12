output "kubeconfig_command" {
  description = "로컬 kubectl 설정 명령어"
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.aws_region}"
}

output "ecr_backend_url" {
  description = "백엔드 ECR URL (CD 파이프라인 참고용)"
  value       = aws_ecr_repository.backend.repository_url
}

output "rds_endpoint" {
  description = "RDS 엔드포인트 (Secrets Manager에 자동 저장됨)"
  value       = module.rds.db_instance_endpoint
  sensitive   = true
}


output "github_actions_role_arn" {
  description = "GitHub Secrets > AWS_ROLE_ARN 에 입력"
  value       = aws_iam_role.github_actions.arn
}

# Amplify는 org deploy-key 제약으로 콘솔 GitHub App으로 연결(amplify.tf.console-managed 참고).
# SPA URL은 Amplify 콘솔 또는 `aws amplify list-apps`로 확인.

output "apigateway_endpoints" {
  description = "환경별 API Gateway invoke URL (프론트 VITE_API_BASE_URL = <endpoint>/api)"
  value = {
    for k, a in aws_apigatewayv2_api.env : k => a.api_endpoint
  }
}
