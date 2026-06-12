package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 피드백 사실 (Star Schema Fact).
 *
 * 운영 feedbacks 테이블에서 적재. 학생별·카테고리별 피드백 건수 집계에 사용.
 * 자연키 feedbackId(운영 feedback.id) 유니크 → ETL upsert 기준.
 * category: GRADE / BEHAVIOR / ATTENDANCE / ATTITUDE / OTHER
 */
@Entity
@Table(name = "fact_feedback",
        uniqueConstraints = @UniqueConstraint(name = "uq_fact_feedback", columnNames = {"feedback_id"}))
@Getter @Setter @NoArgsConstructor
public class FactFeedback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feedback_id", nullable = false)
    private Long feedbackId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 20)
    private String category;
}
