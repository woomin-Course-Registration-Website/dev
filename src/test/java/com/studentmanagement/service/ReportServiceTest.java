package com.studentmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.domain.Student;
import com.studentmanagement.dto.report.ReportPreviewResponse;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock GradeRepository gradeRepository;
    @Mock FeedbackRepository feedbackRepository;
    @Mock CounselingRepository counselingRepository;
    @Mock StudentRecordRepository studentRecordRepository;
    @Spy  ObjectMapper objectMapper;

    @InjectMocks ReportService reportService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = TestFixtures.student(TestFixtures.studentUser());
    }

    // ── getPreview ────────────────────────────────────────────────────

    @Test
    void getPreview_gradeSummary_returnsCorrectColumns() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(gradeRepository.findByStudentAndFilters(anyLong(), anyInt(), anyInt(), isNull()))
                .willReturn(List.of());

        ReportPreviewResponse result = reportService.getPreview("grade-summary", null, null, 2025, 1);

        assertThat(result.getColumns()).extracting("key")
                .contains("name", "avg", "gradeRank", "total");
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    void getPreview_studentRecord_returnsCorrectColumns() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(studentRecordRepository.findByStudentId(10L)).willReturn(Optional.empty());

        ReportPreviewResponse result = reportService.getPreview("student-record", null, null, null, null);

        assertThat(result.getColumns()).extracting("key")
                .contains("name", "present", "absent", "late", "attendanceRate");
    }

    @Test
    void getPreview_feedbackReport_returnsCorrectColumns() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(feedbackRepository.findByStudentIdOrderByCreatedAtDesc(10L)).willReturn(List.of());

        ReportPreviewResponse result = reportService.getPreview("feedback-report", null, null, null, null);

        assertThat(result.getColumns()).extracting("key")
                .contains("name", "total", "publicCount", "gradeCount");
    }

    @Test
    void getPreview_counselingReport_returnsCorrectColumns() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(counselingRepository.findByFilters(anyLong(), isNull(), isNull(), isNull()))
                .willReturn(List.of());

        ReportPreviewResponse result = reportService.getPreview("counseling-report", null, null, null, null);

        assertThat(result.getColumns()).extracting("key")
                .contains("name", "count", "lastDate", "content");
    }

    // ── generateExcel ─────────────────────────────────────────────────

    @Test
    void generateExcel_returnsNonEmptyByteArray() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(gradeRepository.findByStudentAndFilters(anyLong(), anyInt(), anyInt(), isNull()))
                .willReturn(List.of());

        byte[] result = reportService.generateExcel("grade-summary", null, null, 2025, 1);

        assertThat(result).isNotEmpty();
    }

    @Test
    void generateExcel_validWorkbookWithHeaderRow() throws Exception {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(gradeRepository.findByStudentAndFilters(anyLong(), anyInt(), anyInt(), isNull()))
                .willReturn(List.of());

        byte[] bytes = reportService.generateExcel("grade-summary", null, null, 2025, 1);

        try (org.apache.poi.ss.usermodel.Workbook wb =
                org.apache.poi.ss.usermodel.WorkbookFactory.create(
                        new java.io.ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
            // Row 0 = title, Row 1 = empty, Row 2 = header
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(2);
            assertThat(headerRow).isNotNull();
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("이름");
        }
    }

    // ── generatePdf ───────────────────────────────────────────────────

    @Test
    void generatePdf_returnsNonEmptyByteArray() {
        given(studentRepository.findByFilters(null, null, null)).willReturn(List.of(student));
        given(gradeRepository.findByStudentAndFilters(anyLong(), anyInt(), anyInt(), isNull()))
                .willReturn(List.of());

        byte[] result = reportService.generatePdf("grade-summary", null, null, 2025, 1);

        assertThat(result).isNotEmpty();
    }
}
