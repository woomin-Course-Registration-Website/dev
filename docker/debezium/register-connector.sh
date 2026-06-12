#!/usr/bin/env bash
# Debezium MySQL 커넥터 등록 (CDC 가점 기능)
#
# 사용법:
#   1. docker compose --profile cdc up -d           # kafka, kafka-connect 기동
#   2. CDC_ENABLED=true docker compose up -d backend # 백엔드 CDC consumer 활성
#   3. ./docker/debezium/register-connector.sh       # 본 스크립트로 커넥터 등록
#
# kafka-connect REST(8083)에 커넥터 설정(mysql-connector.json)을 POST 한다.
set -euo pipefail

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Waiting for Kafka Connect at ${CONNECT_URL} ..."
until curl -sf "${CONNECT_URL}/connectors" >/dev/null 2>&1; do
  sleep 2
done

echo "Registering Debezium MySQL connector ..."
curl -sf -X POST -H "Content-Type: application/json" \
  --data @"${DIR}/mysql-connector.json" \
  "${CONNECT_URL}/connectors" | tee /dev/stderr

echo
echo "Done. Connector status:"
curl -sf "${CONNECT_URL}/connectors/student-mysql-connector/status"
