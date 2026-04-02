package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 피드백 엔티티
 *
 * 교사가 학생에게 남기는 코멘트를 나타냅니다.
 * isPublic 필드로 학생·학부모의 열람 가능 여부를 제어합니다.
 *
 * - isPublic = true  : TEACHER / STUDENT(본인) / PARENT(자녀) 조회 가능
 * - isPublic = false : 작성한 TEACHER만 조회 가능
 */
@Entity
@Table(name = "feedbacks")
@Getter @Setter @NoArgsConstructor
public class Feedback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 피드백을 작성한 교사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /** 피드백 대상 학생 (학생 삭제 시 피드백도 함께 삭제됨 — CASCADE) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** 피드백 분류: GRADE / BEHAVIOR / ATTENDANCE / ATTITUDE / OTHER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** true이면 학생·학부모도 열람 가능 */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Category { GRADE, BEHAVIOR, ATTENDANCE, ATTITUDE, OTHER }
}
