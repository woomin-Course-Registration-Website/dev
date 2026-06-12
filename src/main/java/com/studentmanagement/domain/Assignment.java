package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 과제 엔티티
 *
 * 교사가 특정 과목·학기에 부여하는 과제를 나타냅니다.
 * 학생별 제출 상태(Submission)와 1:N으로 연결되며, 학습 분석(EP-08)의
 * "과제 제출률" 지표 산출의 원천 데이터가 됩니다.
 */
@Entity
@Table(name = "assignments")
@Getter @Setter @NoArgsConstructor
public class Assignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 200)
    private String title;

    /** 제출 마감일 */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** 학년도 (예: 2024) */
    @Column(nullable = false)
    private int year;

    /** 학기 (1 또는 2) */
    @Column(nullable = false)
    private int semester;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
