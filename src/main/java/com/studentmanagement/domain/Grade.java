package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 성적 엔티티
 *
 * 학생의 특정 과목·연도·학기에 대한 성적을 나타냅니다.
 * 동일한 (학생, 과목, 연도, 학기) 조합은 유니크 제약이 걸려 있어 중복 등록이 불가합니다.
 *
 * gradeRank 자동 계산 기준 (GradeService):
 *   90점 이상 → A, 80점 이상 → B, 70점 이상 → C, 60점 이상 → D, 60점 미만 → F
 */
@Entity
@Table(name = "grades",
        uniqueConstraints = @UniqueConstraint(name = "uq_grade",
                columnNames = {"student_id", "subject_id", "year", "semester"}))
@Getter @Setter @NoArgsConstructor
public class Grade {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /** 학년도 (예: 2025) */
    @Column(nullable = false)
    private int year;

    /** 학기 (1 또는 2) */
    @Column(nullable = false)
    private int semester;

    /** 점수 (0.00 ~ 100.00) */
    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    /** 등급 문자 (A / B / C / D / F) — 점수 저장 시 자동 계산 */
    @Column(name = "grade_rank", length = 2)
    private String gradeRank;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
