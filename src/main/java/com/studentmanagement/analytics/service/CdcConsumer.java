package com.studentmanagement.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.domain.Assignment;
import com.studentmanagement.repository.AssignmentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Kafka CDC Consumer (US-08-06, 가점) — Debezium MySQL 변경 이벤트 → 분석 DB 준실시간 적재.
 *
 * app.analytics.cdc.enabled=true 일 때만 빈으로 등록된다(미설정 시 배치 ETL만 동작).
 * 적재 로직은 {@link EtlService}의 upsert 메서드를 재사용한다.
 *
 * Debezium 토픽: dbserver1.student_management.&lt;table&gt;
 * 메시지 payload: { before, after, op(c/u/d/r), source, ... }
 * (schemas.enable=false 가정. true인 경우 payload 하위에 위치 → 양쪽 모두 처리)
 */
@Component
@ConditionalOnProperty(prefix = "app.analytics.cdc", name = "enabled", havingValue = "true")
public class CdcConsumer {

    private static final Logger log = LoggerFactory.getLogger(CdcConsumer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final EtlService etlService;
    private final AssignmentRepository assignmentRepository;

    public CdcConsumer(EtlService etlService, AssignmentRepository assignmentRepository) {
        this.etlService = etlService;
        this.assignmentRepository = assignmentRepository;
    }

    @KafkaListener(topics = {
            "dbserver1.student_management.students",
            "dbserver1.student_management.subjects",
            "dbserver1.student_management.grades",
            "dbserver1.student_management.student_records",
            "dbserver1.student_management.feedbacks",
            "dbserver1.student_management.submissions"
    })
    public void onMessage(ConsumerRecord<String, String> record) {
        if (record.value() == null) return; // tombstone (삭제) — 본 구현은 분석 측 삭제 미반영
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode envelope = root.has("payload") ? root.get("payload") : root;
            String op = text(envelope, "op");
            if ("d".equals(op)) return; // 삭제 이벤트는 스킵(소프트 정합성 유지)
            JsonNode after = envelope.get("after");
            if (after == null || after.isNull()) return;

            String table = tableOf(record.topic());
            switch (table) {
                case "students" -> handleStudent(after);
                case "subjects" -> handleSubject(after);
                case "grades" -> handleGrade(after);
                case "student_records" -> handleRecord(after);
                case "feedbacks" -> handleFeedback(after);
                case "submissions" -> handleSubmission(after);
                default -> log.debug("[CDC] 미처리 토픽: {}", record.topic());
            }
        } catch (Exception e) {
            log.warn("[CDC] 이벤트 처리 실패 (topic={}): {}", record.topic(), e.getMessage());
        }
    }

    private void handleStudent(JsonNode after) {
        etlService.upsertDimStudent(
                after.get("id").asLong(),
                text(after, "name"),
                after.path("grade").asInt(),
                after.path("class_num").asInt(),
                after.path("student_num").asInt());
    }

    private void handleSubject(JsonNode after) {
        etlService.upsertDimSubject(after.get("id").asLong(), text(after, "name"));
    }

    private void handleGrade(JsonNode after) {
        BigDecimal score = after.hasNonNull("score") ? BigDecimal.valueOf(after.get("score").asDouble()) : null;
        etlService.upsertFactGrade(
                after.get("student_id").asLong(),
                after.get("subject_id").asLong(),
                after.path("year").asInt(),
                after.path("semester").asInt(),
                score,
                text(after, "grade_rank"));
    }

    private void handleRecord(JsonNode after) {
        long studentId = after.get("student_id").asLong();
        int present = 0, absent = 0, late = 0;
        String attendance = text(after, "attendance");
        if (attendance != null && !attendance.isBlank()) {
            try {
                JsonNode att = objectMapper.readTree(attendance);
                present = att.path("present").asInt(0);
                absent = att.path("absent").asInt(0);
                late = att.path("late").asInt(0);
            } catch (Exception ignored) { /* 파싱 실패 시 0 */ }
        }
        etlService.upsertFactAttendance(studentId, present, absent, late);
    }

    private void handleFeedback(JsonNode after) {
        etlService.upsertFactFeedback(
                after.get("id").asLong(),
                after.get("student_id").asLong(),
                text(after, "category"));
    }

    private void handleSubmission(JsonNode after) {
        // submissions CDC에는 과목·학기 정보가 없어 운영 DB의 Assignment에서 보강한다.
        Long assignmentId = after.get("assignment_id").asLong();
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            log.warn("[CDC] 제출 이벤트의 과제를 찾을 수 없음 (assignmentId={})", assignmentId);
            return;
        }
        etlService.upsertFactSubmission(
                assignmentId,
                after.get("student_id").asLong(),
                assignment.getSubject().getId(),
                assignment.getYear(),
                assignment.getSemester(),
                text(after, "status"),
                assignment.getDueDate());
    }

    private String tableOf(String topic) {
        int idx = topic.lastIndexOf('.');
        return idx >= 0 ? topic.substring(idx + 1) : topic;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
