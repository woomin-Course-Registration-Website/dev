package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상담 내역 엔티티
 *
 * 교사가 학생과 진행한 상담 기록을 나타냅니다.
 * shareScope로 학생·학부모 공개 여부를 설정합니다.
 *
 * - ALL     : 학생·학부모도 조회 가능 (향후 기능)
 * - PRIVATE : 교사만 조회 가능
 */
@Entity
@Table(name = "counselings")
@Getter @Setter @NoArgsConstructor
public class Counseling {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상담을 진행한 교사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /** 상담 대상 학생 (학생 삭제 시 상담 내역도 함께 삭제됨 — CASCADE) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** 상담 진행 날짜 */
    @Column(nullable = false)
    private LocalDate date;

    /** 상담 내용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 다음 상담 계획 (선택) */
    @Column(name = "next_plan", columnDefinition = "TEXT")
    private String nextPlan;

    /** 공개 범위: ALL(전체 공개) / PRIVATE(교사 전용) */
    @Enumerated(EnumType.STRING)
    @Column(name = "share_scope", nullable = false, length = 10)
    private ShareScope shareScope = ShareScope.ALL;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum ShareScope { ALL, PRIVATE }
}
