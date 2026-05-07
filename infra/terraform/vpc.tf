locals {
  azs             = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnets  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnets = ["10.0.11.0/24", "10.0.12.0/24"]
  db_subnets      = ["10.0.21.0/24", "10.0.22.0/24"]
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.8"

  name = "${var.project}-vpc"
  cidr = "10.0.0.0/16"

  azs             = local.azs
  public_subnets  = local.public_subnets
  private_subnets = local.private_subnets

  # 고가용성: AZ당 NAT Gateway 1개 (비용 절감 시 single_nat_gateway = true)
  enable_nat_gateway     = true
  single_nat_gateway     = false
  one_nat_gateway_per_az = true

  enable_dns_hostnames = true
  enable_dns_support   = true

  # ALB Ingress Controller가 서브넷을 자동 탐색하기 위한 태그
  public_subnet_tags = {
    "kubernetes.io/role/elb"                       = "1"
    "kubernetes.io/cluster/${var.project}-cluster" = "shared"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb"              = "1"
    "kubernetes.io/cluster/${var.project}-cluster" = "shared"
  }
}

# RDS 전용 서브넷 (EKS 노드 서브넷과 CIDR 분리)
resource "aws_subnet" "db" {
  count             = length(local.azs)
  vpc_id            = module.vpc.vpc_id
  cidr_block        = local.db_subnets[count.index]
  availability_zone = local.azs[count.index]

  tags = {
    Name = "${var.project}-db-${local.azs[count.index]}"
  }
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db-subnet-group"
  subnet_ids = aws_subnet.db[*].id
}

# RDS 보안 그룹: EKS 워커 노드에서만 3306 허용
resource "aws_security_group" "rds" {
  name        = "${var.project}-rds-sg"
  description = "RDS MySQL — EKS 노드 전용 접근"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
