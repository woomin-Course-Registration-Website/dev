#!/bin/bash
set -e

APP_DIR=/home/ubuntu/app

if [ -f "$APP_DIR/docker-compose.prod.yml" ]; then
  cd "$APP_DIR"
  docker compose -f docker-compose.prod.yml down || true
fi
