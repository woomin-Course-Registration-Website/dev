package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 *
 * 시스템에 로그인할 수 있는 모든 사용자를 나타냅니다.
 * 역할(Role)에 따라 접근 가능한 기능이 다릅니다.
 *
 * 역할별 주요 권한:
 * - TEACHER : 학생 등록, 성적/피드백/상담 CRUD
 * - STUDENT : 본인 성적·학생부·공개 피드백 조회
 * - PARENT  : 자녀 성적·학생부·공개 피드백 조회
 * - ADMIN   : 사용자 계정 관리, 과목 추가
 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 및 알림 발송에 사용되는 이메일 (고유값) */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt로 암호화된 비밀번호 */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    /** 사용자 역할 — Spring Security에서 ROLE_{role} 형태로 사용 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    /**
     * 알림 수신 설정 — 성적/피드백/상담 알림 각각 on/off (기본 수신).
     * @ColumnDefault("true")로 prod ddl-auto:update 시 기존 행도 수신(true)으로 채워지며,
     * columnDefinition과 달리 globally_quoted_identifiers(H2 테스트)에서 타입이 깨지지 않는다.
     */
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean notifyGrade = true;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean notifyFeedback = true;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean notifyCounseling = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Role { TEACHER, STUDENT, PARENT, ADMIN }

    public User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
