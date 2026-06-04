# ── API Gateway HTTP API × 3 (환경별 별도) ─────────────────────────────────

locals {
  apigw_envs = {
    dev     = { ns = "student-mgmt-dev" }
    staging = { ns = "student-mgmt-staging" }
    prod    = { ns = "student-mgmt-prod" }
  }
}

# 환경별 private ALB 조회 (aws-load-balancer-controller가 만든 ALB 태그로 식별)
# Argo가 Ingress를 sync한 뒤에야 ALB 존재 → 2-phase apply의 두 번째에서만 resolve
data "aws_lb" "env" {
  for_each = local.apigw_envs

  tags = {
    # IngressGroup이 적용된 ALB의 stack 태그는 group.name과 동일(== ns).
    "ingress.k8s.aws/stack" = each.value.ns
    "elbv2.k8s.aws/cluster" = module.eks.cluster_name
  }
}

data "aws_lb_listener" "env_http" {
  for_each          = data.aws_lb.env
  load_balancer_arn = each.value.arn
  port              = 80
}

# VPC Link 보안그룹 (환경별)
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

  # CORS: 환경별 Amplify 기본 도메인(<branch>.<app-id>.amplifyapp.com)에서만 허용.
  cors_configuration {
    allow_origins = [
      "https://${local.amplify_branches[each.key].branch}.${aws_amplify_app.main.default_domain}"
    ]
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

# 커스텀 도메인 없음: API Gateway의 기본 invoke URL(https://<api-id>.execute-api.<region>.amazonaws.com)을 그대로 사용.

# ALB SG에 VPC Link SG 입력 허용 (aws-load-balancer-controller가 만든 ALB SG 조회 후 룰 추가)
data "aws_security_group" "alb_env" {
  for_each = local.apigw_envs

  filter {
    name   = "tag:elbv2.k8s.aws/cluster"
    values = [module.eks.cluster_name]
  }
  filter {
    name   = "tag:ingress.k8s.aws/stack"
    values = [each.value.ns]
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
