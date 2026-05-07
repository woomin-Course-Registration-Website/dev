terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # 최초 apply 전 수동으로 생성 필요:
  # aws s3 mb s3://student-mgmt-tfstate --region ap-northeast-2
  # aws dynamodb create-table --table-name student-mgmt-tfstate-lock \
  #   --attribute-definitions AttributeName=LockID,AttributeType=S \
  #   --key-schema AttributeName=LockID,KeyType=HASH \
  #   --billing-mode PAY_PER_REQUEST --region ap-northeast-2
  backend "s3" {
    bucket         = "student-mgmt-tfstate"
    key            = "prod/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "student-mgmt-tfstate-lock"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "student-mgmt"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# CloudFront ACM 인증서는 반드시 us-east-1에서 발급
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

# EKS 생성 후 연결 — 최초 apply 시 아래 순서로 실행:
# 1) terraform apply -target=module.eks
# 2) terraform apply
provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_name, "--region", var.aws_region]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_name, "--region", var.aws_region]
    }
  }
}

data "aws_caller_identity" "current" {}

data "aws_availability_zones" "available" {
  state = "available"
}
