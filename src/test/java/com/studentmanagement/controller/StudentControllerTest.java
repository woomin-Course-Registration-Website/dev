package com.studentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.counseling.CounselingResponse;
import com.studentmanagement.dto.student.StudentResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.service.CounselingService;
import com.studentmanagement.service.StudentService;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class StudentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  StudentService studentService;
    @MockBean  CounselingService counselingService;
    @MockBean  JwtUtil jwtUtil;

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getAll_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentService.getAll(any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isForbidden());
    }

    // ── getById ───────────────────────────────────────────────────────

    @Test
    void getById_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        StudentResponse resp = new StudentResponse(
                TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.getById(10L)).willReturn(resp);

        mockMvc.perform(get("/api/students/10").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getById_student_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        StudentResponse resp = new StudentResponse(
                TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.getById(10L)).willReturn(resp);

        mockMvc.perform(get("/api/students/10").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentService.getById(999L)).willThrow(new ResourceNotFoundException("없음"));

        mockMvc.perform(get("/api/students/999").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_teacher_returns201() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        StudentResponse resp = new StudentResponse(
                TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.create(any())).willReturn(resp);

        mockMvc.perform(post("/api/students")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"홍길동","grade":2,"classNum":3,"studentNum":15}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(post("/api/students")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"","grade":5,"classNum":3,"studentNum":15}
                    """))
                .andExpect(status().isBadRequest());
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        StudentResponse resp = new StudentResponse(
                TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.update(anyLong(), any())).willReturn(resp);

        mockMvc.perform(put("/api/students/10")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"수정이름","grade":3,"classNum":1,"studentNum":5}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentService.update(anyLong(), any())).willThrow(new ResourceNotFoundException("없음"));

        mockMvc.perform(put("/api/students/999")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"이름","grade":1,"classNum":1,"studentNum":1}
                    """))
                .andExpect(status().isNotFound());
    }

    // ── getMe ─────────────────────────────────────────────────────────

    @Test
    void getMe_student_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        StudentResponse resp = new StudentResponse(TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.getMyStudent(anyString())).willReturn(resp);

        mockMvc.perform(get("/api/students/me").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(get("/api/students/me").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── getMyChildren ─────────────────────────────────────────────────

    @Test
    void getMyChildren_parent_returns200() throws Exception {
        SecurityTestHelper.stubAsParent(jwtUtil);
        given(studentService.getMyChildren(anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/students/my-children").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getMyChildren_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(get("/api/students/my-children").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── linkParent ────────────────────────────────────────────────────

    @Test
    void linkParent_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        StudentResponse resp = new StudentResponse(TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.linkParent(anyLong(), anyLong())).willReturn(resp);

        mockMvc.perform(post("/api/students/10/parents")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentUserId":3}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void linkParent_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(post("/api/students/10/parents")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentUserId":3}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── unlinkParent ──────────────────────────────────────────────────

    @Test
    void unlinkParent_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        StudentResponse resp = new StudentResponse(TestFixtures.student(TestFixtures.studentUser()));
        given(studentService.unlinkParent(anyLong(), anyLong())).willReturn(resp);

        mockMvc.perform(delete("/api/students/10/parents/3")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void unlinkParent_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(delete("/api/students/10/parents/3")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── getPublicCounselings ──────────────────────────────────────────

    @Test
    void getPublicCounselings_student_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        given(counselingService.getPublicForStudent(anyLong(), anyString(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/counselings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicCounselings_parent_returns200() throws Exception {
        SecurityTestHelper.stubAsParent(jwtUtil);
        given(counselingService.getPublicForStudent(anyLong(), anyString(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/counselings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicCounselings_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(get("/api/students/10/counselings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }
}
