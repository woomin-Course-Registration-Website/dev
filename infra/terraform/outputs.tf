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

output "amplify_app_id" {
  description = "Amplify 앱 ID (콘솔 링크용)"
  value       = aws_amplify_app.main.id
}

output "amplify_branch_urls" {
  description = "환경별 SPA URL (Amplify 기본 도메인)"
  value = {
    for k, v in local.amplify_branches : k => "https://${v.branch}.${aws_amplify_app.main.default_domain}"
  }
}

output "apigateway_endpoints" {
  description = "환경별 API Gateway 기본 invoke URL (frontend VITE_API_BASE_URL에 /api 붙여서 사용)"
  value = {
    for k, v in aws_apigatewayv2_api.env : k => v.api_endpoint
  }
}
