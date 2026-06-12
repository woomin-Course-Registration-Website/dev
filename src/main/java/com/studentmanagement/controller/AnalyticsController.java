package com.studentmanagement.controller;

import com.studentmanagement.analytics.service.AnalyticsService;
import com.studentmanagement.analytics.service.EtlService;
import com.studentmanagement.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 학습 분석 컨트롤러 (EP-08) — TEACHER/ADMIN 전용.
 *
 * 분석 DB(student_analytics)에 적재된 집계 데이터를 조회하고, 배치 ETL을 수동 트리거한다.
 */
@Tag(name = "학습 분석", description = "학생별·과목별 학습 현황 집계 조회 및 ETL 트리거 API (TEACHER/ADMIN)")
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final EtlService etlService;

    public AnalyticsController(AnalyticsService analyticsService, EtlService etlService) {
        this.analyticsService = analyticsService;
        this.etlService = etlService;
    }

    @Operation(summary = "분석 개요", description = "분석 DB에 적재된 학생·과목 목록을 반환합니다(대시보드 진입용).")
    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getOverview()));
    }

    @Operation(
        summary = "학생별 학습 현황",
        description = "성적 추이·출석률·과제 제출률·피드백 분포를 집계하여 반환합니다."
    )
    @GetMapping("/students/{id}/summary")
    public ResponseEntity<?> studentSummary(
            @Parameter(description = "학생 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getStudentSummary(id)));
    }

    @Operation(
        summary = "과목별 학습 현황",
        description = "과목 평균·점수 분포·과제 제출률을 집계하여 반환합니다."
    )
    @GetMapping("/subjects/{id}/distribution")
    public ResponseEntity<?> subjectDistribution(
            @Parameter(description = "과목 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getSubjectDistribution(id)));
    }

    @Operation(
        summary = "ETL 수동 실행",
        description = "운영 DB의 변경분을 분석 DB로 즉시 적재합니다. 스케줄러와 동일한 로직을 사용합니다."
    )
    @PostMapping("/etl/run")
    public ResponseEntity<?> runEtl() {
        return ResponseEntity.ok(ApiResponse.ok(etlService.runEtl(), "ETL 적재가 완료되었습니다."));
    }
}
