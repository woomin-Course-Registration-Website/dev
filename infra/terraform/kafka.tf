# ── Strimzi Kafka Operator (학습 분석 CDC — 가점) ───────────────────────────
# CDC 파이프라인: RDS MySQL binlog → Debezium → Kafka → backend CdcConsumer.
#
# 역할 분담(기존 패턴 유지):
#   • Terraform  = Strimzi "오퍼레이터"(+CRD) 설치 (sealed-secrets·argocd와 동일하게 helm_release)
#   • Argo CD    = 실제 워크로드(Kafka 브로커 CR·Debezium Connect·커넥터 등록 Job) sync
#                  → k8s/kafka 매니페스트, bootstrap 차트의 kafka Application 참고
#
# watchAnyNamespace=true: kafka 네임스페이스의 Kafka/KafkaNodePool CR을 감시.
resource "helm_release" "strimzi_kafka_operator" {
  name             = "strimzi-kafka-operator"
  repository       = "https://strimzi.io/charts/"
  chart            = "strimzi-kafka-operator"
  namespace        = "kafka"
  create_namespace = true
  version          = var.strimzi_chart_version

  set {
    name  = "watchAnyNamespace"
    value = "true"
  }

  depends_on = [module.eks]
}
