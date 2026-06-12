package com.studentmanagement.service;

import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * StudentAccessService 단위 테스트
 *
 * 학생 데이터 접근 권한 결정 로직의 안전망.
 * TEACHER 통과, STUDENT 본인만, PARENT 자녀 한정 — 경계값 모두 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class StudentAccessServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StudentAccessService studentAccessService;

    // ── TEACHER ───────────────────────────────────────────────────────

    @Test
    void check_whenTeacher_passesWithoutLookup() {
        assertThatCode(() -> studentAccessService.check(10L, "teacher@test.com", User.Role.TEACHER))
                .doesNotThrowAnyException();

        // TEACHER 는 무조건 통과하므로 DB 조회조차 일어나지 않아야 한다 (성능 + 권한 모델 분리)
        verify(studentRepository, never()).findById(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void check_whenAdmin_passesAsCurrentlyUnhandled() {
        // 현 구현은 ADMIN을 명시적으로 처리하지 않음 — 통과한다.
        // 의도된 동작이라면 이 테스트가 보호 역할. 추후 ADMIN 제한이 필요하면 회귀 신호가 됨.
        assertThatCode(() -> studentAccessService.check(10L, "admin@test.com", User.Role.ADMIN))
                .doesNotThrowAnyException();
    }

    // ── STUDENT ───────────────────────────────────────────────────────

    @Test
    void check_whenStudentAccessesOwnRecord_succeeds() {
        User studentUser = TestFixtures.studentUser(); // email = "student@test.com"
        Student student = TestFixtures.student(studentUser);
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));

        assertThatCode(() -> studentAccessService.check(10L, "student@test.com", User.Role.STUDENT))
                .doesNotThrowAnyException();
    }

    @Test
    void check_whenStudentAccessesOtherStudent_throwsUnauthorized() {
        User otherStudentUser = TestFixtures.studentUser();
        Student student = TestFixtures.student(otherStudentUser);
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));

        assertThatThrownBy(() -> studentAccessService.check(10L, "different@test.com", User.Role.STUDENT))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void check_whenStudentRecordHasNoLinkedUser_throwsUnauthorized() {
        // 학생 엔티티에 user 연결이 안 된 경우 STUDENT 역할로 접근 불가
        Student orphan = TestFixtures.studentNoUser();
        given(studentRepository.findById(10L)).willReturn(Optional.of(orphan));

        assertThatThrownBy(() -> studentAccessService.check(10L, "student@test.com", User.Role.STUDENT))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void check_whenStudentLookupFails_throwsResourceNotFound() {
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentAccessService.check(999L, "student@test.com", User.Role.STUDENT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── PARENT ────────────────────────────────────────────────────────

    @Test
    void check_whenParentAccessesLinkedChild_succeeds() {
        User parent = TestFixtures.parentUser();
        given(userRepository.findByEmail("parent@test.com")).willReturn(Optional.of(parent));
        given(studentRepository.existsByIdAndParentId(10L, parent.getId())).willReturn(true);

        assertThatCode(() -> studentAccessService.check(10L, "parent@test.com", User.Role.PARENT))
                .doesNotThrowAnyException();
    }

    @Test
    void check_whenParentAccessesUnlinkedStudent_throwsUnauthorized() {
        User parent = TestFixtures.parentUser();
        given(userRepository.findByEmail("parent@test.com")).willReturn(Optional.of(parent));
        given(studentRepository.existsByIdAndParentId(10L, parent.getId())).willReturn(false);

        assertThatThrownBy(() -> studentAccessService.check(10L, "parent@test.com", User.Role.PARENT))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void check_whenParentUserNotFound_throwsResourceNotFound() {
        given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentAccessService.check(10L, "ghost@test.com", User.Role.PARENT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Mockito.any() 보조 ────────────────────────────────────────────

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
