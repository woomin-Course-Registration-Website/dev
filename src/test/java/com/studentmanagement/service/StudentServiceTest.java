package com.studentmanagement.service;

import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.student.StudentRequest;
import com.studentmanagement.dto.student.StudentResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.TestFixtures;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StudentService studentService;

    private User teacher;
    private Student student;

    @BeforeEach
    void setUp() {
        teacher = TestFixtures.teacherUser();
        student = TestFixtures.student(TestFixtures.studentUser());
    }

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getAll_returnsMappedResponses() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));

        List<StudentResponse> result = studentService.getAll(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("홍길동");
    }

    // ── getById ───────────────────────────────────────────────────────

    @Test
    void getById_whenExists_returnsStudentResponse() {
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));

        StudentResponse result = studentService.getById(10L);

        assertThat(result.getName()).isEqualTo("홍길동");
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_withoutUserId_savesWithNullUser() {
        StudentRequest req = studentRequest("김학생", 1, 2, 5, null);
        given(studentRepository.save(any(Student.class))).willAnswer(inv -> {
            Student s = inv.getArgument(0);
            TestFixtures.setId(s, 20L);
            return s;
        });

        StudentResponse result = studentService.create(req);

        assertThat(result.getName()).isEqualTo("김학생");
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void create_withUserId_linksUserAccount() {
        StudentRequest req = studentRequest("김학생", 1, 2, 5, 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
        given(studentRepository.save(any(Student.class))).willAnswer(inv -> {
            Student s = inv.getArgument(0);
            TestFixtures.setId(s, 20L);
            return s;
        });

        StudentResponse result = studentService.create(req);

        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
    }

    @Test
    void create_withInvalidUserId_throwsResourceNotFoundException() {
        StudentRequest req = studentRequest("김학생", 1, 2, 5, 999L);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_whenExists_updatesAllFields() {
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(studentRepository.save(any(Student.class))).willAnswer(inv -> inv.getArgument(0));
        StudentRequest req = studentRequest("새이름", 3, 1, 10, null);

        StudentResponse result = studentService.update(10L, req);

        assertThat(result.getName()).isEqualTo("새이름");
        assertThat(student.getGrade()).isEqualTo(3);
        assertThat(student.getClassNum()).isEqualTo(1);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFoundException() {
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(999L, studentRequest("이름", 1, 1, 1, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private StudentRequest studentRequest(String name, int grade, int classNum, int studentNum, Long userId) {
        StudentRequest r = new StudentRequest();
        setField(r, "name", name);
        setField(r, "grade", grade);
        setField(r, "classNum", classNum);
        setField(r, "studentNum", studentNum);
        setField(r, "userId", userId);
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
