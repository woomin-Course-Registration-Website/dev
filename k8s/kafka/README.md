# 학습 분석 CDC — EKS 배포 (Strimzi)

운영 DB(RDS MySQL)의 변경을 **Debezium → Kafka → 백엔드 `CdcConsumer`** 경로로 분석 DB(`student_analytics`)에 준실시간 적재한다. 배치 ETL(`@Scheduled`)이 항상 동작하는 기반선이고, CDC는 그 위에 얹는 **가점/옵션** 기능이다.

## 구성 요소

| 리소스 | 위치 | 설치 주체 |
|--------|------|-----------|
| Strimzi 오퍼레이터 + CRD | `kafka` ns | **Terraform** `helm_release.strimzi_kafka_operator` ([infra/terraform/kafka.tf](../../infra/terraform/kafka.tf)) |
| Kafka 브로커 (KRaft 단일 노드) | `kafka` ns | **Argo** (`Kafka`/`KafkaNodePool` CR) |
| Debezium Connect (`quay.io/debezium/connect:2.7`) | `kafka` ns | **Argo** (Deployment+Service) |
| 커넥터 등록 Job | `kafka` ns | **Argo** PostSync 훅 |
| `kafka-debezium-secret` (RDS 접속) | `kafka` ns | 운영자 `kubeseal` → SealedSecret |
| dev 백엔드 CDC 토글 | `student-mgmt-dev` ns | Kustomize `overlays/dev/cdc-patch.yaml` |

Argo Application(`kafka-cdc`)은 bootstrap 차트(`k8s/bootstrap`)가 `kafka.enabled=true`일 때 생성하며 이 디렉터리(`k8s/kafka`)를 sync한다.

> **범위**: 비용 최소화를 위해 Kafka 브로커는 단일 노드, CDC 소비는 **dev 환경에서만** 활성(`cdc-patch.yaml`). staging/prod는 배치 ETL만 동작.

## 컷오버 절차 (1회)

1. **RDS binlog 활성** — [rds.tf](../../infra/terraform/rds.tf)에 `binlog_format=ROW`, `binlog_row_image=FULL`(pending-reboot)이 이미 반영됨. `terraform apply` 후 **RDS 재부팅**으로 적용. 이어서 binlog 보존시간 설정:
   ```sql
   CALL mysql.rds_set_configuration('binlog retention hours', 24);
   ```
2. **Debezium용 DB 사용자** 생성(또는 앱 사용자에 권한 부여):
   ```sql
   CREATE USER 'debezium'@'%' IDENTIFIED BY '<pw>';
   GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium'@'%';
   ```
   (RDS에서 `RELOAD`가 막히면 커넥터에 `"snapshot.locking.mode":"none"` 추가)
3. **시크릿 봉인** — [sealed-secrets/kustomization.yaml](sealed-secrets/kustomization.yaml)의 안내대로 `kafka-debezium-secret` SealedSecret을 생성·커밋.
4. **분석 DB** — `CREATE DATABASE IF NOT EXISTS student_analytics;` (배치 ETL과 공용; 스타 스키마 테이블은 `app.analytics.ddl-auto`가 생성).
5. git push → Argo가 `kafka-cdc` Application을 sync → 브로커·Connect 기동 → PostSync Job이 커넥터 등록 → dev 백엔드 `CdcConsumer`가 토픽 구독 시작.

## 동작 확인

```bash
# Connect 커넥터 상태
kubectl -n kafka exec deploy/debezium-connect -- \
  curl -s localhost:8083/connectors/student-mysql-connector/status

# 토픽 목록 (dbserver1.student_management.* 가 생성되어야 함)
kubectl -n kafka exec -it student-kafka-dual-role-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# 백엔드 CDC 소비 로그
kubectl -n student-mgmt-dev logs deploy/backend | grep -i cdc
```

## 비활성화

`k8s/bootstrap/values.yaml`에서 `kafka.enabled=false` → Argo가 `kafka-cdc` Application 제거. 백엔드는 `cdc-patch.yaml`만 빼면 배치 ETL만 남는다. Strimzi 오퍼레이터 자체 제거는 `terraform destroy -target=helm_release.strimzi_kafka_operator`.
