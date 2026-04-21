package com.studentmanagement.service;

import com.studentmanagement.domain.Notification;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.notification.NotificationResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.NotificationRepository;
import com.studentmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks NotificationService notificationService;

    private User teacher;
    private Notification notification;

    @BeforeEach
    void setUp() {
        teacher      = TestFixtures.teacherUser();
        notification = TestFixtures.notification(teacher);
    }

    // ── getMyNotifications ────────────────────────────────────────────

    @Test
    void getMyNotifications_returnsOrderedList() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getMyNotifications("teacher@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).contains("수학 성적");
    }

    @Test
    void getMyNotifications_whenUserNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getMyNotifications("unknown@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── markAsRead ────────────────────────────────────────────────────

    @Test
    void markAsRead_whenOwner_setsReadTrue() {
        given(notificationRepository.findById(500L)).willReturn(Optional.of(notification));

        notificationService.markAsRead(500L, "teacher@test.com");

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_whenNotOwner_throwsUnauthorizedException() {
        given(notificationRepository.findById(500L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(500L, "other@test.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void markAsRead_whenNotificationNotFound_throwsResourceNotFoundException() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, "teacher@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── markAllAsRead ─────────────────────────────────────────────────

    @Test
    void markAllAsRead_callsBulkUpdate() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));

        notificationService.markAllAsRead("teacher@test.com");

        verify(notificationRepository).markAllAsRead(1L);
    }

    @Test
    void markAllAsRead_whenUserNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAllAsRead("unknown@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── send ──────────────────────────────────────────────────────────

    @Test
    void send_whenRecipientNotNull_savesNotification() {
        notificationService.send(teacher, Notification.Type.GRADE, "성적 알림");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void send_whenRecipientIsNull_doesNotSave() {
        notificationService.send(null, Notification.Type.GRADE, "알림");

        verify(notificationRepository, never()).save(any());
    }
}
