package com.studentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.counseling.CounselingResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.service.CounselingService;
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

@WebMvcTest(CounselingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class CounselingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  CounselingService counselingService;
    @MockBean  JwtUtil jwtUtil;

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getAll_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(counselingService.getAll(any(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/counselings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/counselings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_admin_returns403() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);

        mockMvc.perform(get("/api/counselings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_teacher_returns201() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        CounselingResponse resp = new CounselingResponse(
                TestFixtures.counseling(TestFixtures.teacherUser(),
                        TestFixtures.student(TestFixtures.studentUser())));
        given(counselingService.create(any(), anyString())).willReturn(resp);

        mockMvc.perform(post("/api/counselings")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":10,"date":"2025-03-10","content":"상담 내용","nextPlan":"다음 계획","shareScope":"ALL"}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void create_admin_returns403() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);

        mockMvc.perform(post("/api/counselings")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":10,"date":"2025-03-10","content":"상담 내용","nextPlan":"다음 계획","shareScope":"ALL"}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        CounselingResponse resp = new CounselingResponse(
                TestFixtures.counseling(TestFixtures.teacherUser(),
                        TestFixtures.student(TestFixtures.studentUser())));
        given(counselingService.update(anyLong(), any(), anyString())).willReturn(resp);

        mockMvc.perform(put("/api/counselings/400")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":10,"date":"2025-03-10","content":"수정 내용","nextPlan":"수정 계획","shareScope":"PRIVATE"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void update_notAuthor_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(counselingService.update(anyLong(), any(), anyString()))
                .willThrow(new UnauthorizedException("작성자 아님"));

        mockMvc.perform(put("/api/counselings/400")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":10,"date":"2025-03-10","content":"수정 내용","nextPlan":"수정 계획","shareScope":"ALL"}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(counselingService).delete(anyLong(), anyString());

        mockMvc.perform(delete("/api/counselings/400").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willThrow(new ResourceNotFoundException("없음"))
                .given(counselingService).delete(anyLong(), anyString());

        mockMvc.perform(delete("/api/counselings/999").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isNotFound());
    }
}
