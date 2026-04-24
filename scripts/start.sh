#!/bin/bash
set -e

APP_DIR=/home/ubuntu/app
cd "$APP_DIR"

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
docker image prune -f
