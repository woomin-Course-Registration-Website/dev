package com.studentmanagement.service;

import com.studentmanagement.domain.Notification;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.notification.NotificationResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.repository.NotificationRepository;
import com.studentmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 서비스
 *
 * 알림 조회·읽음 처리 및 알림 생성(send)을 담당합니다.
 *
 * send() 메서드는 다른 서비스에서 이벤트 발생 시 호출합니다.
 * 예) GradeService에서 성적 입력 후 → send(student.getUser(), GRADE, "성적이 등록되었습니다.")
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * 내 알림 목록 조회
     * JWT subject(이메일)로 사용자를 식별하여 본인 알림만 반환합니다.
     * 최신순(createdAt DESC)으로 정렬됩니다.
     */
    public List<NotificationResponse> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(NotificationResponse::new).toList();
    }

    /**
     * 알림 1개 읽음 처리
     * 본인 소유 여부를 이메일로 검증합니다.
     *
     * @throws UnauthorizedException 본인 알림이 아닌 경우
     */
    @Transactional
    public void markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getEmail().equals(email)) {
            throw new UnauthorizedException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        notification.setRead(true);
    }

    /**
     * 전체 알림 읽음 처리
     * JPQL UPDATE 쿼리로 한 번에 처리합니다 (엔티티 개별 로딩 없음).
     */
    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        notificationRepository.markAllAsRead(user.getId());
    }

    /**
     * 알림 생성 (내부 호출용)
     *
     * 성적·피드백·상담 등록 시 서비스 레이어에서 호출합니다.
     * recipient가 null(계정 미연동 학생)이면 알림을 생성하지 않습니다.
     *
     * @param recipient 알림 수신 사용자
     * @param type      알림 종류 (GRADE / FEEDBACK / COUNSELING)
     * @param message   알림 메시지
     */
    @Transactional
    public void send(User recipient, Notification.Type type, String message) {
        if (recipient == null) return;
        notificationRepository.save(new Notification(recipient, type, message));
    }
}
