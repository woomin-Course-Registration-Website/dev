#!/bin/bash
# EC2 Ubuntu 초기 설정 스크립트
# 사용법: sudo bash ec2-setup.sh <AWS_REGION>
# 예시:   sudo bash ec2-setup.sh ap-northeast-2
set -e

AWS_REGION=${1:?"AWS_REGION을 인자로 전달하세요. 예: sudo bash ec2-setup.sh ap-northeast-2"}
APP_DIR=/home/ubuntu/app
APP_USER=ubuntu

echo "=== [1/4] Docker 설치 ==="
apt-get update -y
apt-get install -y ca-certificates curl gnupg

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable docker
systemctl start docker
usermod -aG docker "$APP_USER"

echo "=== [2/4] CodeDeploy 에이전트 설치 ==="
apt-get install -y ruby-full wget

cd /tmp
wget -q "https://aws-codedeploy-${AWS_REGION}.s3.${AWS_REGION}.amazonaws.com/latest/install"
chmod +x ./install
./install auto

systemctl enable codedeploy-agent
systemctl start codedeploy-agent

echo "=== [3/4] 앱 디렉토리 생성 ==="
mkdir -p "$APP_DIR"
chown "$APP_USER":"$APP_USER" "$APP_DIR"

echo "=== [4/4] .env 템플릿 생성 ==="
ENV_FILE="$APP_DIR/.env"
if [ ! -f "$ENV_FILE" ]; then
  cat > "$ENV_FILE" <<'EOF'
DOCKERHUB_USERNAME=
DB_ROOT_PASSWORD=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
EOF
  chown "$APP_USER":"$APP_USER" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  echo ".env 템플릿 생성 완료: $ENV_FILE — 값을 채워주세요."
else
  echo ".env 파일이 이미 존재합니다. 건너뜁니다."
fi

echo ""
echo "=== 설치 완료 ==="
echo "Docker:         $(docker --version)"
echo "CodeDeploy:     $(systemctl is-active codedeploy-agent)"
echo ""
echo "[주의] docker 그룹 적용을 위해 재접속하거나 'newgrp docker' 실행 필요"
echo "[주의] $ENV_FILE 에 실제 값을 입력해야 배포가 정상 동작합니다."
