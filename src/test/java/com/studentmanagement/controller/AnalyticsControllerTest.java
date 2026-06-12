package com.studentmanagement.controller;

import com.studentmanagement.analytics.service.AnalyticsService;
import com.studentmanagement.analytics.service.EtlService;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.analytics.AnalyticsOverviewResponse;
import com.studentmanagement.dto.analytics.EtlResult;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class AnalyticsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  AnalyticsService analyticsService;
    @MockBean  EtlService etlService;
    @MockBean  JwtUtil jwtUtil;

    @Test
    void overview_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(analyticsService.getOverview())
                .willReturn(new AnalyticsOverviewResponse(List.of(), List.of()));

        mockMvc.perform(get("/api/analytics/overview").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void overview_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/analytics/overview").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void overview_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(analyticsService.getOverview())
                .willReturn(new AnalyticsOverviewResponse(List.of(), List.of()));

        mockMvc.perform(get("/api/analytics/overview").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void studentSummary_notFound_returns404() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(analyticsService.getStudentSummary(anyLong()))
                .willThrow(new ResourceNotFoundException("없음"));

        mockMvc.perform(get("/api/analytics/students/999/summary").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void runEtl_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(etlService.runEtl()).willReturn(new EtlResult(7, 5, 70, 7, 70, 7));

        mockMvc.perform(post("/api/analytics/etl/run").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void runEtl_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(post("/api/analytics/etl/run").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }
}
