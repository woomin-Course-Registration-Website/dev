package com.studentmanagement.service;

import com.studentmanagement.domain.*;
import com.studentmanagement.dto.grade.GradeRequest;
import com.studentmanagement.dto.grade.GradeResponse;
import com.studentmanagement.dto.grade.GradeStatsItem;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.GradeRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock GradeRepository gradeRepository;
    @Mock StudentRepository studentRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock NotificationService notificationService;
    @Mock StudentAccessService studentAccessService;

    @InjectMocks GradeService gradeService;

    private User studentUser;
    private Student student;
    private Student orphanStudent;
    private Subject subject;

    @BeforeEach
    void setUp() {
        studentUser   = TestFixtures.studentUser();
        student       = TestFixtures.student(studentUser);
        orphanStudent = TestFixtures.studentNoUser();
        subject       = TestFixtures.subject();
    }

    // ── getGrades ─────────────────────────────────────────────────────

    @Test
    void getGrades_whenTeacher_returnsGradesWithoutAccessCheck() {
        Grade grade = buildGrade(student, "85.00", "B");
        given(gradeRepository.findByStudentAndFilters(10L, null, null, null))
                .willReturn(List.of(grade));
        stubAverageAndTotal();

        List<GradeResponse> result = gradeService.getGrades(10L, null, null, null,
                "teacher@test.com", User.Role.TEACHER);

        assertThat(result).hasSize(1);
        verify(studentAccessService).check(10L, "teacher@test.com", User.Role.TEACHER);
    }

    @Test
    void getGrades_whenStudent_callsAccessCheckAndReturnsGrades() {
        Grade grade = buildGrade(student, "85.00", "B");
        given(gradeRepository.findByStudentAndFilters(10L, null, null, null))
                .willReturn(List.of(grade));
        stubAverageAndTotal();

        List<GradeResponse> result = gradeService.getGrades(10L, null, null, null,
                "student@test.com", User.Role.STUDENT);

        assertThat(result).hasSize(1);
        verify(studentAccessService).check(10L, "student@test.com", User.Role.STUDENT);
    }

    @Test
    void getGrades_whenAccessDenied_throwsUnauthorizedException() {
        willThrow(new com.studentmanagement.exception.UnauthorizedException("권한 없음"))
                .given(studentAccessService).check(10L, "other@test.com", User.Role.STUDENT);

        assertThatThrownBy(() -> gradeService.getGrades(10L, null, null, null,
                "other@test.com", User.Role.STUDENT))
                .isInstanceOf(com.studentmanagement.exception.UnauthorizedException.class);
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_whenValidInput_savesGradeAndSendsNotification() {
        GradeRequest req = gradeRequest("85.00");
        stubStudentAndSubject(student);
        Grade saved = buildGrade(student, "85.00", "B");
        given(gradeRepository.save(any())).willReturn(saved);
        stubAverageAndTotal();

        GradeResponse result = gradeService.create(10L, req);

        assertThat(result.getGradeRank()).isEqualTo("B");
        verify(notificationService).send(eq(studentUser), eq(Notification.Type.GRADE), anyString());
    }

    @Test
    void create_whenStudentNotFound_throwsResourceNotFoundException() {
        given(studentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.create(99L, gradeRequest("85.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생");
    }

    @Test
    void create_whenSubjectNotFound_throwsResourceNotFoundException() {
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(subjectRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.create(10L, gradeRequest("85.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("과목");
    }

    @Test
    void create_whenStudentHasNoUser_notificationSentWithNullRecipient() {
        GradeRequest req = gradeRequest("80.00");
        given(studentRepository.findById(10L)).willReturn(Optional.of(orphanStudent));
        given(subjectRepository.findById(100L)).willReturn(Optional.of(subject));
        Grade saved = buildGrade(orphanStudent, "80.00", "B");
        given(gradeRepository.save(any())).willReturn(saved);
        stubAverageAndTotal();

        gradeService.create(10L, req);

        verify(notificationService).send(isNull(), eq(Notification.Type.GRADE), anyString());
    }

    // ── calculateRank 경계값 ──────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "90.00, A",
        "89.99, B",
        "80.00, B",
        "79.99, C",
        "70.00, C",
        "69.99, D",
        "60.00, D",
        "59.99, F"
    })
    void create_gradeRankCalculatedCorrectly(String score, String expectedRank) {
        stubStudentAndSubject(student);
        Grade saved = buildGrade(student, score, expectedRank);
        given(gradeRepository.save(any())).willReturn(saved);
        stubAverageAndTotal();

        GradeResponse result = gradeService.create(10L, gradeRequest(score));

        assertThat(result.getGradeRank()).isEqualTo(expectedRank);
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_whenGradeExists_recalculatesRank() {
        Grade existing = buildGrade(student, "85.00", "B");
        given(gradeRepository.findById(200L)).willReturn(Optional.of(existing));
        given(gradeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        stubAverageAndTotal();

        GradeResponse result = gradeService.update(200L, gradeRequest("95.00"));

        assertThat(result.getGradeRank()).isEqualTo("A");
        assertThat(result.getScore()).isEqualByComparingTo("95.00");
    }

    @Test
    void update_whenGradeNotFound_throwsResourceNotFoundException() {
        given(gradeRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.update(999L, gradeRequest("80.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_whenGradeExists_succeeds() {
        given(gradeRepository.existsById(200L)).willReturn(true);

        assertThatCode(() -> gradeService.delete(200L)).doesNotThrowAnyException();
        verify(gradeRepository).deleteById(200L);
    }

    @Test
    void delete_whenGradeNotFound_throwsResourceNotFoundException() {
        given(gradeRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> gradeService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getStats ──────────────────────────────────────────────────────

    @Test
    void getStats_calculatesEnteredCountCorrectly() {
        given(studentRepository.findByFilters(2, 3, null)).willReturn(List.of(student));
        given(subjectRepository.findAll()).willReturn(List.of(subject));
        given(gradeRepository.countBySubjectYearSemesterAndStudents(any(), anyInt(), anyInt(), any()))
                .willReturn(1L);

        List<GradeStatsItem> stats = gradeService.getStats(2, 3, 2025, 1);

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getGradeCount()).isEqualTo(1L);
        assertThat(stats.get(0).getStudentCount()).isEqualTo(1L);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private void stubStudentAndSubject(Student s) {
        given(studentRepository.findById(10L)).willReturn(Optional.of(s));
        given(subjectRepository.findById(100L)).willReturn(Optional.of(subject));
    }

    private void stubAverageAndTotal() {
        given(gradeRepository.findAverageScore(any(), anyInt(), anyInt())).willReturn(80.0);
        given(gradeRepository.findTotalScore(any(), anyInt(), anyInt())).willReturn(85.0);
    }

    private GradeRequest gradeRequest(String score) {
        GradeRequest r = new GradeRequest();
        setField(r, "subjectId", 100L);
        setField(r, "year", 2025);
        setField(r, "semester", 1);
        setField(r, "score", new BigDecimal(score));
        return r;
    }

    private Grade buildGrade(Student s, String score, String rank) {
        Grade g = TestFixtures.grade(s, subject);
        g.setScore(new BigDecimal(score));
        g.setGradeRank(rank);
        return g;
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
