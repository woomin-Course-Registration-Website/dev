package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 과제 제출 엔티티
 *
 * 특정 과제(Assignment)에 대한 학생별 제출 상태를 나타냅니다.
 * 동일 (과제, 학생) 조합은 유니크 제약이 걸려 중복 기록이 불가합니다.
 *
 * status:
 *   - SUBMITTED     : 기한 내 제출
 *   - LATE          : 지각 제출
 *   - NOT_SUBMITTED : 미제출
 */
@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_submission",
                columnNames = {"assignment_id", "student_id"}))
@Getter @Setter @NoArgsConstructor
public class Submission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOT_SUBMITTED;

    /** 제출 시각 (미제출이면 null) */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Status { SUBMITTED, LATE, NOT_SUBMITTED }
}
