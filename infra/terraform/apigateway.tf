# ── API Gateway HTTP API × 3 (환경별 별도) ─────────────────────────────────

locals {
  apigw_envs = {
    dev     = { prefix = "api-dev", spa_prefix = "dev", ns = "student-mgmt-dev" }
    staging = { prefix = "api-staging", spa_prefix = "staging", ns = "student-mgmt-staging" }
    prod    = { prefix = "api", spa_prefix = "", ns = "student-mgmt-prod" }
  }
}

# 환경별 private ALB 조회 (aws-load-balancer-controller가 만든 ALB 태그로 식별)
# Argo가 Ingress를 sync한 뒤에야 ALB 존재 → 2-phase apply의 두 번째에서만 resolve
data "aws_lb" "env" {
  for_each = local.apigw_envs

  tags = {
    "ingress.k8s.aws/stack" = "${each.value.ns}/main"
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

  cors_configuration {
    allow_origins = [
      each.value.spa_prefix == "" ? "https://${var.domain_name}" : "https://${each.value.spa_prefix}.${var.domain_name}"
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

# ALB SG에 VPC Link SG 입력 허용 (aws-load-balancer-controller가 만든 ALB SG 조회 후 룰 추가)
data "aws_security_group" "alb_env" {
  for_each = local.apigw_envs

  filter {
    name   = "tag:elbv2.k8s.aws/cluster"
    values = [module.eks.cluster_name]
  }
  filter {
    name   = "tag:ingress.k8s.aws/stack"
    values = ["${each.value.ns}/main"]
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
