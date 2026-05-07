resource "aws_secretsmanager_secret" "db" {
  name                    = "${var.project}/db"
  description             = "RDS 접속 정보"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id

  secret_string = jsonencode({
    username = var.db_username
    password = var.db_password
    endpoint = module.rds.db_instance_endpoint
    dbname   = "student_management"
    # K8s ExternalSecret이 이 값을 SPRING_DATASOURCE_URL로 주입
    jdbc_url = "jdbc:mysql://${module.rds.db_instance_endpoint}/student_management?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true"
  })
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.project}/jwt"
  description             = "JWT 서명 키"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = jsonencode({ secret = var.jwt_secret })
}
