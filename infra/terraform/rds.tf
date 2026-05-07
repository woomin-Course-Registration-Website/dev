module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.6"

  identifier = "${var.project}-db"

  engine               = "mysql"
  engine_version       = "8.0"
  family               = "mysql8.0"
  major_engine_version = "8.0"
  instance_class       = "db.t3.medium"

  allocated_storage     = 20
  max_allocated_storage = 100 # Storage Autoscaling 상한

  db_name  = "student_management"
  username = var.db_username
  password = var.db_password
  port     = 3306

  # 고가용성: 다른 AZ에 스탠바이 인스턴스 유지
  multi_az = true

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period   = 7
  backup_window             = "03:00-04:00"
  maintenance_window        = "Mon:04:00-Mon:05:00"

  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.project}-db-final-snapshot"

  performance_insights_enabled          = true
  performance_insights_retention_period = 7

  parameters = [
    { name = "character_set_client",  value = "utf8mb4" },
    { name = "character_set_server",  value = "utf8mb4" },
    { name = "collation_server",      value = "utf8mb4_unicode_ci" },
    { name = "time_zone",             value = "Asia/Seoul" },
  ]
}
