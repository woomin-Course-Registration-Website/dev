package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 학생 엔티티
 *
 * 교사가 관리하는 학생 정보를 나타냅니다.
 * User 엔티티와는 선택적으로 연동됩니다.
 * - user가 null : 학생 계정이 없는 경우 (교사가 직접 관리)
 * - user가 존재 : 학생 본인이 시스템에 로그인 가능한 경우
 */
@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor
public class Student {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연동된 사용자 계정 (선택)
     * 계정 삭제 시 이 필드는 NULL로 변경됩니다 (ON DELETE SET NULL).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    /** 학년 (1 ~ 3) */
    @Column(nullable = false)
    private int grade;

    /** 반 번호 */
    @Column(name = "class_num", nullable = false)
    private int classNum;

    /** 반 내 번호 */
    @Column(name = "student_num", nullable = false)
    private int studentNum;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "student_parents",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "parent_user_id")
    )
    private Set<User> parents = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Student(String name, int grade, int classNum, int studentNum) {
        this.name = name;
        this.grade = grade;
        this.classNum = classNum;
        this.studentNum = studentNum;
    }
}
