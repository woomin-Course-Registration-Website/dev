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

# ── 환경별 시크릿 컨테이너 (값은 라이브 컷오버 시 주입) ──────────────────────

resource "aws_secretsmanager_secret" "env_db" {
  for_each                = toset(["dev", "staging", "prod"])
  name                    = "${var.project}/${each.value}/db"
  description             = "${each.value} 환경 RDS 접속 정보"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret" "env_jwt" {
  for_each                = toset(["dev", "staging", "prod"])
  name                    = "${var.project}/${each.value}/jwt"
  description             = "${each.value} 환경 JWT 서명 키"
  recovery_window_in_days = 7
}

# ── Argo CD 시크릿 컨테이너 ──────────────────────────────────────────────────

resource "aws_secretsmanager_secret" "argocd_admin" {
  name                    = "${var.project}/argocd/admin"
  description             = "Argo CD admin 비밀번호 (bcrypt) + passwordMtime"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret" "argocd_repo" {
  name                    = "${var.project}/argocd/repo"
  description             = "Argo CD private git repo 자격증명 (url/username/token)"
  recovery_window_in_days = 7
}
