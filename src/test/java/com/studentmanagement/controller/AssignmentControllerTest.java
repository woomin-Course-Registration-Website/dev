package com.studentmanagement.controller;

import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.service.AssignmentService;
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

@WebMvcTest(AssignmentController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class AssignmentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  AssignmentService assignmentService;
    @MockBean  JwtUtil jwtUtil;

    @Test
    void listBySubject_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(assignmentService.getBySubject(anyLong())).willReturn(List.of());

        mockMvc.perform(get("/api/subjects/1/assignments").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void listBySubject_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/subjects/1/assignments").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordSubmission_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(assignmentService.recordSubmission(anyLong(), any())).willReturn(null);

        mockMvc.perform(post("/api/assignments/5/submissions")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":10,"status":"SUBMITTED"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void create_validationError_returns400() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(post("/api/subjects/1/assignments")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"","dueDate":null,"year":null,"semester":null}
                    """))
                .andExpect(status().isBadRequest());
    }
}
