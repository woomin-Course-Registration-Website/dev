package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.report.ReportPreviewResponse;
import com.studentmanagement.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 보고서 생성 컨트롤러
 * - GET /api/reports/preview  : 미리보기 JSON 데이터
 * - GET /api/reports/download : Excel / PDF 파일 다운로드
 */
@Tag(name = "보고서", description = "성적·학생부·피드백·상담 보고서 생성 및 다운로드 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * 보고서 미리보기 데이터 조회 (JSON)
     *
     * @param type     보고서 종류 (grade-summary | student-record | feedback-report | counseling-report)
     * @param grade    학년 필터 (선택)
     * @param classNum 반 필터 (선택)
     * @param year     연도 (성적 보고서에서 사용)
     * @param semester 학기 (성적 보고서에서 사용)
     */
    @Operation(
        summary = "보고서 미리보기",
        description = "보고서 종류와 조건에 맞는 데이터를 JSON으로 반환합니다. " +
                      "type: grade-summary | student-record | feedback-report | counseling-report"
    )
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<ReportPreviewResponse>> getPreview(
            @Parameter(description = "보고서 종류") @RequestParam String type,
            @Parameter(description = "학년 필터")  @RequestParam(required = false) Integer grade,
            @Parameter(description = "반 필터")    @RequestParam(required = false) Integer classNum,
            @Parameter(description = "연도")       @RequestParam(required = false) Integer year,
            @Parameter(description = "학기 (1 또는 2)") @RequestParam(required = false) Integer semester) {

        ReportPreviewResponse preview = reportService.getPreview(type, grade, classNum, year, semester);
        return ResponseEntity.ok(ApiResponse.ok(preview));
    }

    /**
     * 보고서 파일 다운로드 (Excel 또는 PDF)
     *
     * @param format excel (기본값) 또는 pdf
     */
    @Operation(
        summary = "보고서 파일 다운로드",
        description = "Excel(.xlsx) 또는 PDF 형식으로 보고서를 다운로드합니다. " +
                      "format: excel(기본) | pdf"
    )
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "보고서 종류") @RequestParam String type,
            @Parameter(description = "학년 필터")  @RequestParam(required = false) Integer grade,
            @Parameter(description = "반 필터")    @RequestParam(required = false) Integer classNum,
            @Parameter(description = "연도")       @RequestParam(required = false) Integer year,
            @Parameter(description = "학기")       @RequestParam(required = false) Integer semester,
            @Parameter(description = "파일 형식 (excel | pdf)") @RequestParam(defaultValue = "excel") String format) {

        byte[] fileBytes;
        String contentType;
        String filename = String.format("%s_%s", type, LocalDate.now());

        if ("pdf".equalsIgnoreCase(format)) {
            fileBytes   = reportService.generatePdf(type, grade, classNum, year, semester);
            contentType = "application/pdf";
            filename   += ".pdf";
        } else {
            fileBytes   = reportService.generateExcel(type, grade, classNum, year, semester);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename   += ".xlsx";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(fileBytes);
    }
}
