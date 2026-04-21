package com.studentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.feedback.FeedbackResponse;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.service.FeedbackService;
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

@WebMvcTest(FeedbackController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class FeedbackControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  FeedbackService feedbackService;
    @MockBean  JwtUtil jwtUtil;

    // ── getFeedbacks ──────────────────────────────────────────────────

    @Test
    void getFeedbacks_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(feedbackService.getFeedbacks(anyLong(), anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/feedbacks").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getFeedbacks_student_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        given(feedbackService.getFeedbacks(anyLong(), anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/feedbacks").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getFeedbacks_parent_returns200() throws Exception {
        SecurityTestHelper.stubAsParent(jwtUtil);
        given(feedbackService.getFeedbacks(anyLong(), anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/students/10/feedbacks").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void createFeedback_teacher_returns201() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        FeedbackResponse resp = new FeedbackResponse(
                TestFixtures.feedback(TestFixtures.teacherUser(), TestFixtures.student(TestFixtures.studentUser())));
        given(feedbackService.create(anyLong(), any(), anyString())).willReturn(resp);

        mockMvc.perform(post("/api/students/10/feedbacks")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"category":"GRADE","content":"피드백 내용","isPublic":true}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void createFeedback_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(post("/api/students/10/feedbacks")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"category":"GRADE","content":"피드백 내용","isPublic":true}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void updateFeedback_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        FeedbackResponse resp = new FeedbackResponse(
                TestFixtures.feedback(TestFixtures.teacherUser(), TestFixtures.student(TestFixtures.studentUser())));
        given(feedbackService.update(anyLong(), any(), anyString())).willReturn(resp);

        mockMvc.perform(put("/api/feedbacks/300")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"category":"GRADE","content":"수정 내용","isPublic":true}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void updateFeedback_notAuthor_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(feedbackService.update(anyLong(), any(), anyString()))
                .willThrow(new UnauthorizedException("작성자 아님"));

        mockMvc.perform(put("/api/feedbacks/300")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"category":"GRADE","content":"수정 내용","isPublic":true}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void deleteFeedback_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(feedbackService).delete(anyLong(), anyString());

        mockMvc.perform(delete("/api/feedbacks/300").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteFeedback_notAuthor_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willThrow(new UnauthorizedException("작성자 아님"))
                .given(feedbackService).delete(anyLong(), anyString());

        mockMvc.perform(delete("/api/feedbacks/300").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }
}
