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
    dev     = { branch = "develop", prefix = "dev" }
    staging = { branch = "staging", prefix = "staging" }
    prod    = { branch = "main", prefix = "" }
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
