module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.11"

  cluster_name    = "${var.project}-cluster"
  cluster_version = "1.30"

  cluster_endpoint_public_access       = true
  cluster_endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_addons = {
    coredns    = { most_recent = true }
    kube-proxy = { most_recent = true }
    vpc-cni = {
      most_recent              = true
      service_account_role_arn = module.vpc_cni_irsa.iam_role_arn
      # NetworkPolicy 강제 적용 활성화 (미설정 시 매니페스트의 NetworkPolicy가 무시됨)
      configuration_values = jsonencode({
        enableNetworkPolicy = "true"
      })
    }
    aws-ebs-csi-driver = {
      most_recent              = true
      service_account_role_arn = module.ebs_csi_irsa.iam_role_arn
    }
  }

  eks_managed_node_groups = {
    main = {
      name           = "${var.project}-nodegroup"
      instance_types = [var.eks_node_instance_type]
      capacity_type  = "ON_DEMAND"

      min_size     = var.eks_node_min
      max_size     = var.eks_node_max
      desired_size = var.eks_node_desired

      block_device_mappings = {
        xvda = {
          device_name = "/dev/xvda"
          ebs = {
            volume_size           = 50
            volume_type           = "gp3"
            delete_on_termination = true
          }
        }
      }
    }
  }

  enable_cluster_creator_admin_permissions = true
}

# ── IRSA (IAM Roles for Service Accounts) ─────────────────────────────────

module "vpc_cni_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.39"

  role_name             = "${var.project}-vpc-cni"
  attach_vpc_cni_policy = true
  vpc_cni_enable_ipv4   = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:aws-node"]
    }
  }
}

module "ebs_csi_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.39"

  role_name             = "${var.project}-ebs-csi"
  attach_ebs_csi_policy = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:ebs-csi-controller-sa"]
    }
  }
}

module "lbc_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.39"

  role_name                              = "${var.project}-aws-lbc"
  attach_load_balancer_controller_policy = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:aws-load-balancer-controller"]
    }
  }
}


module "cluster_autoscaler_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.39"

  role_name                        = "${var.project}-cluster-autoscaler"
  attach_cluster_autoscaler_policy = true
  cluster_autoscaler_cluster_names = [module.eks.cluster_name]

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:cluster-autoscaler"]
    }
  }
}

# ── Helm Releases ──────────────────────────────────────────────────────────

resource "helm_release" "aws_lbc" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace  = "kube-system"
  version    = "1.8.1"

  set {
    name  = "clusterName"
    value = module.eks.cluster_name
  }
  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = module.lbc_irsa.iam_role_arn
  }

  depends_on = [module.eks]
}


resource "helm_release" "cluster_autoscaler" {
  name       = "cluster-autoscaler"
  repository = "https://kubernetes.github.io/autoscaler"
  chart      = "cluster-autoscaler"
  namespace  = "kube-system"
  version    = "9.37.0"

  set {
    name  = "autoDiscovery.clusterName"
    value = module.eks.cluster_name
  }
  set {
    name  = "awsRegion"
    value = var.aws_region
  }
  set {
    name  = "rbac.serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = module.cluster_autoscaler_irsa.iam_role_arn
  }

  depends_on = [module.eks]
}

# HPA 동작에 필요한 메트릭 서버
resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  namespace  = "kube-system"
  version    = "3.12.1"

  depends_on = [module.eks]
}


# ── Prometheus + Grafana (앱 메트릭 스크랩) ──────────────────────────────────
# Grafana는 Ingress 미설정 — 접근 시 `kubectl -n monitoring port-forward svc/kube-prometheus-stack-grafana 3000:80`
resource "helm_release" "kube_prometheus_stack" {
  name             = "kube-prometheus-stack"
  repository       = "https://prometheus-community.github.io/helm-charts"
  chart            = "kube-prometheus-stack"
  namespace        = "monitoring"
  create_namespace = true
  version          = "65.1.1"

  values = [yamlencode({
    prometheus = {
      prometheusSpec = {
        # student-mgmt 네임스페이스의 ServiceMonitor도 스크랩하도록 라벨 셀렉터 해제
        serviceMonitorSelectorNilUsesHelmValues = false
        podMonitorSelectorNilUsesHelmValues     = false
      }
    }

    alertmanager = {
      alertmanagerSpec = {
        # 'alertmanager-discord-webhook' SealedSecret을 Pod의
        # /etc/alertmanager/secrets/alertmanager-discord-webhook/url 경로로 마운트.
        # 운영자가 컷오버 시 Discord webhook URL + '/slack' 값을 채워 SealedSecret 생성.
        secrets = ["alertmanager-discord-webhook"]
      }
      config = {
        global = {
          resolve_timeout = "5m"
        }
        route = {
          receiver        = "discord"
          group_by        = ["alertname", "namespace"]
          group_wait      = "30s"
          group_interval  = "5m"
          repeat_interval = "4h"
        }
        receivers = [
          {
            name = "discord"
            # Discord 웹훅 URL 끝에 '/slack'을 붙이면 Slack-포맷 페이로드를 받아준다 →
            # Alertmanager의 slack_configs를 그대로 활용 (전용 프록시 불필요).
            slack_configs = [{
              api_url_file  = "/etc/alertmanager/secrets/alertmanager-discord-webhook/url"
              send_resolved = true
              title         = "[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}"
              text          = <<-EOT
                {{ range .Alerts }}
                *Severity:* {{ .Labels.severity }}
                *Namespace:* {{ .Labels.namespace }}
                *Summary:* {{ .Annotations.summary }}
                *Description:* {{ .Annotations.description }}
                {{ end }}
              EOT
            }]
          }
        ]
      }
    }

    grafana = {
      # Loki를 추가 데이터소스로 등록 → Grafana에서 메트릭+로그 통합 조회.
      additionalDataSources = [{
        name      = "Loki"
        type      = "loki"
        url       = "http://loki.monitoring.svc.cluster.local:3100"
        access    = "proxy"
        isDefault = false
      }]
    }
  })]

  depends_on = [module.eks]
}

# ── Loki + Promtail (컨테이너 로그 집계) ─────────────────────────────────────

resource "helm_release" "loki" {
  name             = "loki"
  repository       = "https://grafana.github.io/helm-charts"
  chart            = "loki"
  namespace        = "monitoring"
  create_namespace = true
  version          = "6.16.0"

  values = [yamlencode({
    # 단일 노드 SingleBinary 모드 (소규모, 비용 최소화)
    deploymentMode = "SingleBinary"
    loki = {
      auth_enabled = false
      commonConfig = {
        replication_factor = 1
      }
      storage = {
        type = "filesystem"
      }
      schemaConfig = {
        configs = [{
          from         = "2024-01-01"
          store        = "tsdb"
          object_store = "filesystem"
          schema       = "v13"
          index = {
            prefix = "loki_index_"
            period = "24h"
          }
        }]
      }
      # 로그 보존 7일 (저비용)
      limits_config = {
        retention_period = "168h"
      }
    }
    singleBinary = {
      replicas = 1
      persistence = {
        enabled = true
        size    = "20Gi"
      }
    }
    # 비활성 (SingleBinary 모드)
    backend      = { replicas = 0 }
    read         = { replicas = 0 }
    write        = { replicas = 0 }
    chunksCache  = { enabled = false }
    resultsCache = { enabled = false }
  })]

  depends_on = [module.eks]
}

resource "helm_release" "promtail" {
  name       = "promtail"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "promtail"
  namespace  = "monitoring"
  version    = "6.16.6"

  values = [yamlencode({
    config = {
      clients = [{
        url = "http://loki.monitoring.svc.cluster.local:3100/loki/api/v1/push"
      }]
    }
  })]

  depends_on = [helm_release.loki]
}

# ── Sealed Secrets ─────────────────────────────────────────────────────────

resource "helm_release" "sealed_secrets" {
  name             = "sealed-secrets"
  repository       = "https://bitnami-labs.github.io/sealed-secrets"
  chart            = "sealed-secrets"
  namespace        = "sealed-secrets"
  create_namespace = true
  version          = var.sealed_secrets_chart_version

  depends_on = [module.eks]
}

# ── Argo CD (GitOps) ─────────────────────────────────────────────────────────

resource "helm_release" "argocd" {
  name             = "argocd"
  repository       = "https://argoproj.github.io/argo-helm"
  chart            = "argo-cd"
  namespace        = "argocd"
  create_namespace = true
  version          = var.argocd_chart_version

  # 공용 노출 없음. UI는 port-forward 전용:
  # kubectl port-forward svc/argocd-server -n argocd 8080:443
  # 초기 admin 비밀번호:
  # kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d

  depends_on = [module.eks]
}

resource "helm_release" "argocd_bootstrap" {
  name      = "argocd-bootstrap"
  chart     = "${path.module}/../../k8s/bootstrap"
  namespace = "argocd"

  set {
    name  = "repoURL"
    value = "https://github.com/${var.github_org}/${var.github_repo}.git"
  }

  depends_on = [helm_release.argocd]
}
