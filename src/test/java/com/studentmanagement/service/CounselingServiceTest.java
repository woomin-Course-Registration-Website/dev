package com.studentmanagement.service;

import com.studentmanagement.domain.*;
import com.studentmanagement.dto.counseling.CounselingRequest;
import com.studentmanagement.dto.counseling.CounselingResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.CounselingRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CounselingServiceTest {

    @Mock CounselingRepository counselingRepository;
    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock StudentAccessService studentAccessService;

    @InjectMocks CounselingService counselingService;

    private User teacher;
    private User studentUser;
    private Student student;
    private Counseling counseling;

    @BeforeEach
    void setUp() {
        teacher     = TestFixtures.teacherUser();
        studentUser = TestFixtures.studentUser();
        student     = TestFixtures.student(studentUser);
        counseling  = TestFixtures.counseling(teacher, student);
    }

    // ── getPublicForStudent ───────────────────────────────────────────

    @Test
    void getPublicForStudent_whenStudent_returnsPublicCounselings() {
        given(counselingRepository.findPublicByStudentId(10L)).willReturn(List.of(counseling));

        List<CounselingResponse> result = counselingService.getPublicForStudent(
                10L, "student@test.com", User.Role.STUDENT);

        assertThat(result).hasSize(1);
        verify(studentAccessService).check(10L, "student@test.com", User.Role.STUDENT);
    }

    @Test
    void getPublicForStudent_whenParent_returnsPublicCounselings() {
        given(counselingRepository.findPublicByStudentId(10L)).willReturn(List.of(counseling));

        List<CounselingResponse> result = counselingService.getPublicForStudent(
                10L, "parent@test.com", User.Role.PARENT);

        assertThat(result).hasSize(1);
        verify(studentAccessService).check(10L, "parent@test.com", User.Role.PARENT);
    }

    @Test
    void getPublicForStudent_whenAccessDenied_throwsUnauthorizedException() {
        willThrow(new com.studentmanagement.exception.UnauthorizedException("권한 없음"))
                .given(studentAccessService).check(10L, "other@test.com", User.Role.STUDENT);

        assertThatThrownBy(() -> counselingService.getPublicForStudent(
                10L, "other@test.com", User.Role.STUDENT))
                .isInstanceOf(com.studentmanagement.exception.UnauthorizedException.class);
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_whenValid_savesAndSendsNotification() {
        CounselingRequest req = counselingRequest(Counseling.ShareScope.ALL);
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(counselingRepository.save(any())).willReturn(counseling);

        CounselingResponse result = counselingService.create(req, "teacher@test.com");

        assertThat(result).isNotNull();
        verify(notificationService).send(eq(studentUser), eq(Notification.Type.COUNSELING), anyString());
    }

    @Test
    void create_whenNoShareScope_defaultsToAll() {
        CounselingRequest req = counselingRequest(null);
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(counselingRepository.save(any())).willAnswer(inv -> {
            Counseling c = inv.getArgument(0);
            assertThat(c.getShareScope()).isEqualTo(Counseling.ShareScope.ALL);
            return counseling;
        });

        counselingService.create(req, "teacher@test.com");
    }

    @Test
    void create_whenTeacherNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> counselingService.create(counselingRequest(null), "unknown@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_whenStudentNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(studentRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> counselingService.create(counselingRequest(null), "teacher@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_whenAuthor_updatesFields() {
        given(counselingRepository.findById(400L)).willReturn(Optional.of(counseling));
        given(counselingRepository.save(any())).willReturn(counseling);
        CounselingRequest req = counselingRequest(Counseling.ShareScope.PRIVATE);

        CounselingResponse result = counselingService.update(400L, req, "teacher@test.com");

        assertThat(result).isNotNull();
        assertThat(counseling.getShareScope()).isEqualTo(Counseling.ShareScope.PRIVATE);
    }

    @Test
    void update_whenNotAuthor_throwsUnauthorizedException() {
        given(counselingRepository.findById(400L)).willReturn(Optional.of(counseling));

        assertThatThrownBy(() -> counselingService.update(400L, counselingRequest(null), "other@test.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFoundException() {
        given(counselingRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> counselingService.update(999L, counselingRequest(null), "teacher@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_whenAuthor_succeeds() {
        given(counselingRepository.findById(400L)).willReturn(Optional.of(counseling));

        assertThatCode(() -> counselingService.delete(400L, "teacher@test.com"))
                .doesNotThrowAnyException();
        verify(counselingRepository).delete(counseling);
    }

    @Test
    void delete_whenNotAuthor_throwsUnauthorizedException() {
        given(counselingRepository.findById(400L)).willReturn(Optional.of(counseling));

        assertThatThrownBy(() -> counselingService.delete(400L, "other@test.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private CounselingRequest counselingRequest(Counseling.ShareScope scope) {
        CounselingRequest r = new CounselingRequest();
        setField(r, "studentId", 10L);
        setField(r, "date", LocalDate.of(2025, 3, 10));
        setField(r, "content", "학업 상담 내용");
        setField(r, "nextPlan", "다음 달 재상담");
        setField(r, "shareScope", scope);
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
