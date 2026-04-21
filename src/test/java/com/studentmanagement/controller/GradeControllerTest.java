package com.studentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.domain.Grade;
import com.studentmanagement.domain.Subject;
import com.studentmanagement.dto.grade.GradeResponse;
import com.studentmanagement.dto.grade.GradeStatsItem;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.service.GradeService;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GradeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class GradeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  GradeService gradeService;
    @MockBean  JwtUtil jwtUtil;

    // ── getGrades ─────────────────────────────────────────────────────

    @Test
    void getGrades_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(gradeService.getGrades(anyLong(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getGrades_student_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        given(gradeService.getGrades(anyLong(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getGrades_parent_returns200() throws Exception {
        SecurityTestHelper.stubAsParent(jwtUtil);
        given(gradeService.getGrades(anyLong(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getGrades_admin_returns403() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);

        mockMvc.perform(get("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void getGrades_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/students/10/grades"))
                .andExpect(status().isForbidden());
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void createGrade_teacher_returns201() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        GradeResponse resp = gradeResponse();
        given(gradeService.create(anyLong(), any())).willReturn(resp);

        mockMvc.perform(post("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subjectId":100,"year":2025,"semester":1,"score":85.00}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void createGrade_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(post("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subjectId":100,"year":2025,"semester":1,"score":85.00}
                    """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGrade_invalidScore_returns400() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(post("/api/students/10/grades")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subjectId":100,"year":2025,"semester":1,"score":-5}
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGrade_studentNotFound_returns404() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(gradeService.create(anyLong(), any()))
                .willThrow(new ResourceNotFoundException("학생 없음"));

        mockMvc.perform(post("/api/students/99/grades")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subjectId":100,"year":2025,"semester":1,"score":85.00}
                    """))
                .andExpect(status().isNotFound());
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void updateGrade_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(gradeService.update(anyLong(), any())).willReturn(gradeResponse());

        mockMvc.perform(put("/api/grades/200")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subjectId":100,"year":2025,"semester":1,"score":90.00}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void updateGrade_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(put("/api/grades/200")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subjectId":100,"year":2025,"semester":1,"score":90.00}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void deleteGrade_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(gradeService).delete(anyLong());

        mockMvc.perform(delete("/api/grades/200")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteGrade_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(delete("/api/grades/200")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── getStats ──────────────────────────────────────────────────────

    @Test
    void getStats_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(gradeService.getStats(any(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/grades/stats")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(gradeService.getStats(any(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/grades/stats")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/grades/stats")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────

    private GradeResponse gradeResponse() {
        Subject subject = TestFixtures.subject();
        Grade grade = TestFixtures.grade(
                TestFixtures.student(TestFixtures.studentUser()), subject);
        return new GradeResponse(grade, 80.0, 85.0);
    }
}
