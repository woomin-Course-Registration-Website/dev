# ── AWS Amplify (React SPA 호스팅) ──────────────────────────────────────────

resource "aws_amplify_app" "main" {
  name         = var.project
  repository   = "https://github.com/${var.github_org}/${var.github_repo}"
  access_token = var.amplify_github_token
  platform     = "WEB"

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

  enable_branch_auto_build    = true
  enable_branch_auto_deletion = false

  # SPA fallback: 알 수 없는 경로는 index.html로 (React Router 대응)
  custom_rule {
    source = "/<*>"
    target = "/index.html"
    status = "404-200"
  }
}

locals {
  amplify_branches = {
    dev     = { branch = "develop" }
    staging = { branch = "staging" }
    prod    = { branch = "main" }
  }
}

resource "aws_amplify_branch" "env" {
  for_each = local.amplify_branches

  app_id      = aws_amplify_app.main.id
  branch_name = each.value.branch
  framework   = "React"
  stage       = each.key == "prod" ? "PRODUCTION" : "DEVELOPMENT"

  enable_auto_build = true

  # API Gateway 기본 invoke URL을 그대로 사용 (커스텀 도메인 없음).
  # frontend client.js가 baseURL = '/api'를 기대하므로 끝에 /api를 붙임.
  environment_variables = {
    VITE_API_BASE_URL = "${aws_apigatewayv2_api.env[each.key].api_endpoint}/api"
  }
}
