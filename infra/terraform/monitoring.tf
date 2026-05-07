resource "aws_cloudwatch_log_group" "eks_cluster" {
  name              = "/aws/eks/${var.project}-cluster/cluster"
  retention_in_days = 30
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/aws/eks/${var.project}-cluster/application"
  retention_in_days = 14
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${var.project}-rds-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "RDS CPU 사용률 80% 초과"

  dimensions = {
    DBInstanceIdentifier = module.rds.db_instance_identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "rds_connections" {
  alarm_name          = "${var.project}-rds-connections-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = 60
  statistic           = "Maximum"
  threshold           = 80
  alarm_description   = "RDS 연결 수 80 초과"

  dimensions = {
    DBInstanceIdentifier = module.rds.db_instance_identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "rds_storage" {
  alarm_name          = "${var.project}-rds-storage-low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Minimum"
  threshold           = 5368709120 # 5GB (bytes)
  alarm_description   = "RDS 남은 스토리지 5GB 미만"

  dimensions = {
    DBInstanceIdentifier = module.rds.db_instance_identifier
  }
}

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = var.project

  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        properties = {
          title   = "RDS CPU 사용률"
          metrics = [["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", module.rds.db_instance_identifier]]
          period  = 300
          stat    = "Average"
          view    = "timeSeries"
        }
      },
      {
        type = "metric"
        properties = {
          title   = "RDS 연결 수"
          metrics = [["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", module.rds.db_instance_identifier]]
          period  = 60
          stat    = "Maximum"
          view    = "timeSeries"
        }
      },
      {
        type = "metric"
        properties = {
          title   = "RDS 남은 스토리지"
          metrics = [["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", module.rds.db_instance_identifier]]
          period  = 300
          stat    = "Minimum"
          view    = "timeSeries"
        }
      }
    ]
  })
}
