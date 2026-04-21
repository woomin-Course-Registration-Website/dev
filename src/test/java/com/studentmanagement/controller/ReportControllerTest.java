package com.studentmanagement.controller;

import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.report.ReportPreviewResponse;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.service.ReportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  ReportService reportService;
    @MockBean  JwtUtil jwtUtil;

    // ── getPreview ────────────────────────────────────────────────────

    @Test
    void getPreview_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(reportService.getPreview(any(), any(), any(), any(), any()))
                .willReturn(previewResponse());

        mockMvc.perform(get("/api/reports/preview")
                .param("type", "grade-summary")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getPreview_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(reportService.getPreview(any(), any(), any(), any(), any()))
                .willReturn(previewResponse());

        mockMvc.perform(get("/api/reports/preview")
                .param("type", "grade-summary")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getPreview_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/reports/preview")
                .param("type", "grade-summary")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── download ──────────────────────────────────────────────────────

    @Test
    void downloadExcel_teacher_returns200WithXlsxContentType() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(reportService.generateExcel(any(), any(), any(), any(), any()))
                .willReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reports/download")
                .param("type", "grade-summary")
                .param("format", "excel")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void downloadPdf_teacher_returns200WithPdfContentType() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(reportService.generatePdf(any(), any(), any(), any(), any()))
                .willReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reports/download")
                .param("type", "grade-summary")
                .param("format", "pdf")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void download_contentDispositionHeaderPresent() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(reportService.generateExcel(any(), any(), any(), any(), any()))
                .willReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reports/download")
                .param("type", "grade-summary")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    // ── helpers ───────────────────────────────────────────────────────

    private ReportPreviewResponse previewResponse() {
        return ReportPreviewResponse.builder()
                .columns(List.of())
                .rows(List.of())
                .totalCount(0)
                .generatedAt("2025-03-10")
                .build();
    }
}
