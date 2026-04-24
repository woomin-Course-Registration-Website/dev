#!/bin/bash
set -e

for i in $(seq 1 10); do
  if curl -sf http://localhost:8080/api/health > /dev/null; then
    echo "Health check passed"
    exit 0
  fi
  echo "Waiting for service... ($i/10)"
  sleep 3
done

echo "Health check failed"
exit 1
