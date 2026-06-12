package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 과제 제출 사실 (Star Schema Fact).
 *
 * 운영 submissions 테이블에서 적재. (assignment, student)별 제출 상태.
 * 자연키 (assignmentId, studentId) 유니크 → ETL upsert 기준.
 * status: SUBMITTED / LATE / NOT_SUBMITTED
 */
@Entity
@Table(name = "fact_submission",
        uniqueConstraints = @UniqueConstraint(name = "uq_fact_submission",
                columnNames = {"assignment_id", "student_id"}))
@Getter @Setter @NoArgsConstructor
public class FactSubmission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "date_key", nullable = false)
    private Integer dateKey;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;
}
