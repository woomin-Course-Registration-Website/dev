package com.studentmanagement.service;

import com.studentmanagement.domain.*;
import com.studentmanagement.dto.feedback.FeedbackRequest;
import com.studentmanagement.dto.feedback.FeedbackResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.FeedbackRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock FeedbackRepository feedbackRepository;
    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    @InjectMocks FeedbackService feedbackService;

    private User teacher;
    private User studentUser;
    private Student student;
    private Student orphanStudent;
    private Feedback publicFeedback;
    private Feedback privateFeedback;

    @BeforeEach
    void setUp() {
        teacher        = TestFixtures.teacherUser();
        studentUser    = TestFixtures.studentUser();
        student        = TestFixtures.student(studentUser);
        orphanStudent  = TestFixtures.studentNoUser();
        publicFeedback = TestFixtures.feedback(teacher, student);
        privateFeedback = TestFixtures.privateFeedback(teacher, student);
    }

    // ── getFeedbacks ──────────────────────────────────────────────────

    @Test
    void getFeedbacks_whenRequesterIsTeacher_returnsAll() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(feedbackRepository.findByStudentIdOrderByCreatedAtDesc(10L))
                .willReturn(List.of(publicFeedback, privateFeedback));

        List<FeedbackResponse> result = feedbackService.getFeedbacks(10L, "teacher@test.com");

        assertThat(result).hasSize(2);
    }

    @Test
    void getFeedbacks_whenRequesterIsStudent_returnsOnlyPublic() {
        given(userRepository.findByEmail("student@test.com")).willReturn(Optional.of(studentUser));
        given(feedbackRepository.findPublicByStudentId(10L))
                .willReturn(List.of(publicFeedback));

        List<FeedbackResponse> result = feedbackService.getFeedbacks(10L, "student@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublic()).isTrue();
    }

    @Test
    void getFeedbacks_whenRequesterIsParent_returnsOnlyPublic() {
        User parent = TestFixtures.parentUser();
        given(userRepository.findByEmail("parent@test.com")).willReturn(Optional.of(parent));
        given(feedbackRepository.findPublicByStudentId(10L))
                .willReturn(List.of(publicFeedback));

        List<FeedbackResponse> result = feedbackService.getFeedbacks(10L, "parent@test.com");

        assertThat(result).hasSize(1);
    }

    @Test
    void getFeedbacks_whenRequesterNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.getFeedbacks(10L, "unknown@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_whenValid_savesAndSendsNotification() {
        FeedbackRequest req = feedbackRequest();
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(feedbackRepository.save(any())).willReturn(publicFeedback);

        FeedbackResponse result = feedbackService.create(10L, req, "teacher@test.com");

        assertThat(result).isNotNull();
        verify(notificationService).send(eq(studentUser), eq(Notification.Type.FEEDBACK), anyString());
    }

    @Test
    void create_whenStudentHasNoUser_notificationSentWithNullRecipient() {
        FeedbackRequest req = feedbackRequest();
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(studentRepository.findById(10L)).willReturn(Optional.of(orphanStudent));
        Feedback f = TestFixtures.feedback(teacher, orphanStudent);
        given(feedbackRepository.save(any())).willReturn(f);

        feedbackService.create(10L, req, "teacher@test.com");

        verify(notificationService).send(isNull(), eq(Notification.Type.FEEDBACK), anyString());
    }

    @Test
    void create_whenStudentNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(studentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.create(99L, feedbackRequest(), "teacher@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_whenAuthor_updatesFeedback() {
        given(feedbackRepository.findById(300L)).willReturn(Optional.of(publicFeedback));
        FeedbackRequest req = feedbackRequest();

        FeedbackResponse result = feedbackService.update(300L, req, "teacher@test.com");

        assertThat(result).isNotNull();
    }

    @Test
    void update_whenNotAuthor_throwsUnauthorizedException() {
        given(feedbackRepository.findById(300L)).willReturn(Optional.of(publicFeedback));

        assertThatThrownBy(() -> feedbackService.update(300L, feedbackRequest(), "other@test.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void update_whenFeedbackNotFound_throwsResourceNotFoundException() {
        given(feedbackRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.update(999L, feedbackRequest(), "teacher@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_whenAuthor_deletesFeedback() {
        given(feedbackRepository.findById(300L)).willReturn(Optional.of(publicFeedback));

        assertThatCode(() -> feedbackService.delete(300L, "teacher@test.com"))
                .doesNotThrowAnyException();
        verify(feedbackRepository).delete(publicFeedback);
    }

    @Test
    void delete_whenNotAuthor_throwsUnauthorizedException() {
        given(feedbackRepository.findById(300L)).willReturn(Optional.of(publicFeedback));

        assertThatThrownBy(() -> feedbackService.delete(300L, "other@test.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private FeedbackRequest feedbackRequest() {
        FeedbackRequest r = new FeedbackRequest();
        setField(r, "category", Feedback.Category.GRADE);
        setField(r, "content", "피드백 내용");
        setField(r, "isPublic", true);
        return r;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
