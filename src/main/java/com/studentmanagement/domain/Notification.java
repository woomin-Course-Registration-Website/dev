package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 *
 * 시스템 이벤트 발생 시 사용자에게 전송되는 알림을 나타냅니다.
 * NotificationService.send()를 통해 서비스 레이어에서 자동 생성됩니다.
 *
 * 알림이 생성되는 시점:
 * - GRADE      : 교사가 성적을 입력할 때 → 학생·학부모에게 발송
 * - FEEDBACK   : 교사가 피드백을 작성할 때 → 학생에게 발송
 * - COUNSELING : 교사가 상담을 등록할 때 → 학생에게 발송
 */
@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림 수신자 (사용자 삭제 시 알림도 함께 삭제됨 — CASCADE) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 알림 종류: GRADE / FEEDBACK / COUNSELING */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** 알림 메시지 내용 (최대 500자) */
    @Column(nullable = false, length = 500)
    private String message;

    /** 읽음 여부 — 기본값 false (읽지 않음) */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Type { GRADE, FEEDBACK, COUNSELING }

    public Notification(User user, Type type, String message) {
        this.user = user;
        this.type = type;
        this.message = message;
    }
}
