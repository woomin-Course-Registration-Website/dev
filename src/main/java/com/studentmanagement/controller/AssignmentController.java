package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.assignment.AssignmentRequest;
import com.studentmanagement.dto.assignment.SubmissionRequest;
import com.studentmanagement.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 과제 관리 컨트롤러 (TEACHER 전용)
 *
 * - 과목별 과제 등록·조회
 * - 학생별 제출 상태 기록(upsert)·조회
 *
 * 이 데이터는 학습 분석(EP-08)의 과제 제출률 지표 원천이 됩니다.
 */
@Tag(name = "과제 관리", description = "교사 과제 등록·조회 및 학생 제출 상태 기록 API (TEACHER 전용)")
@RestController
@PreAuthorize("hasRole('TEACHER')")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "과목별 과제 목록", description = "특정 과목의 과제 목록을 마감일 내림차순으로 반환합니다.")
    @GetMapping("/api/subjects/{subjectId}/assignments")
    public ResponseEntity<?> listBySubject(
            @Parameter(description = "과목 ID") @PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getBySubject(subjectId)));
    }

    @Operation(summary = "과제 등록", description = "특정 과목에 새 과제를 등록합니다.")
    @PostMapping("/api/subjects/{subjectId}/assignments")
    public ResponseEntity<?> create(
            @Parameter(description = "과목 ID") @PathVariable Long subjectId,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(assignmentService.create(subjectId, request)));
    }

    @Operation(summary = "과제 제출 목록", description = "특정 과제의 학생별 제출 상태 목록을 반환합니다.")
    @GetMapping("/api/assignments/{assignmentId}/submissions")
    public ResponseEntity<?> listSubmissions(
            @Parameter(description = "과제 ID") @PathVariable Long assignmentId) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getSubmissions(assignmentId)));
    }

    @Operation(
        summary = "제출 상태 기록",
        description = "학생별 제출 상태를 기록/갱신합니다(upsert).\n\n" +
                      "**status 값:** `SUBMITTED`(기한 내) / `LATE`(지각) / `NOT_SUBMITTED`(미제출)"
    )
    @PostMapping("/api/assignments/{assignmentId}/submissions")
    public ResponseEntity<?> recordSubmission(
            @Parameter(description = "과제 ID") @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.recordSubmission(assignmentId, request)));
    }
}
