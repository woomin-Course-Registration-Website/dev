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

  # 디스크 평문 저장 금지 (개인정보보호법 정렬, 기본 AWS 관리 KMS 키)
  storage_encrypted = true

  db_name  = "student_management"
  username = var.db_username
  password = var.db_password
  port     = 3306

  # 고가용성: 다른 AZ에 스탠바이 인스턴스 유지
  multi_az = true

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"

  deletion_protection              = false # teardown: 삭제 보호 해제
  skip_final_snapshot              = true  # teardown: 최종 스냅샷 생략 (백업 불필요)
  final_snapshot_identifier_prefix = "${var.project}-db-final-snapshot"

  performance_insights_enabled          = true
  performance_insights_retention_period = 7

  parameters = [
    { name = "character_set_client", value = "utf8mb4" },
    { name = "character_set_server", value = "utf8mb4" },
    { name = "collation_server", value = "utf8mb4_unicode_ci" },
    { name = "time_zone", value = "Asia/Seoul" },
    # Debezium CDC(가점)용 binlog 설정 — ROW 포맷 + FULL row image.
    # 적용에 재부팅 필요(pending-reboot). binlog 보존시간은 파라미터가 아니므로
    # 컷오버 시 SQL로 1회: CALL mysql.rds_set_configuration('binlog retention hours', 24);
    # CDC 미사용 환경에도 무해(자동 백업이 켜져 있어 binlog는 어차피 생성됨).
    { name = "binlog_format", value = "ROW", apply_method = "pending-reboot" },
    { name = "binlog_row_image", value = "FULL", apply_method = "pending-reboot" },
  ]
}
