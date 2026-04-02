package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.counseling.CounselingRequest;
import com.studentmanagement.service.CounselingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 상담 내역 관리 컨트롤러
 *
 * - 교사가 학생과 진행한 상담 내역을 기록·관리합니다.
 * - shareScope로 공개 범위를 설정합니다.
 *   - ALL     : 학생·학부모도 열람 가능 (현재 API는 TEACHER만 조회)
 *   - PRIVATE : 교사만 열람 가능
 * - 수정/삭제는 작성한 교사 본인만 가능합니다.
 */
@Tag(name = "상담 관리", description = "교사 상담 내역 등록·조회·수정·삭제 API (TEACHER 전용)")
@RestController
@RequestMapping("/api/counselings")
@PreAuthorize("hasRole('TEACHER')")
public class CounselingController {

    private final CounselingService counselingService;

    public CounselingController(CounselingService counselingService) {
        this.counselingService = counselingService;
    }

    @Operation(
        summary = "상담 목록 조회",
        description = "상담 내역 목록을 반환합니다. 복합 필터링이 가능합니다.\n\n" +
                      "**필터 파라미터 (모두 선택)**\n" +
                      "- `studentId`: 특정 학생의 상담만 조회\n" +
                      "- `teacherId`: 특정 교사가 진행한 상담만 조회\n" +
                      "- `from` / `to`: 날짜 범위 필터 (형식: `yyyy-MM-dd`)\n\n" +
                      "결과는 상담 날짜 내림차순으로 정렬됩니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "TEACHER 권한 없음")
    })
    @GetMapping
    public ResponseEntity<?> getAll(
            @Parameter(description = "학생 ID 필터") @RequestParam(required = false) Long studentId,
            @Parameter(description = "교사 ID 필터") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(counselingService.getAll(studentId, teacherId, from, to)));
    }

    @Operation(summary = "상담 상세 조회", description = "특정 상담 내역의 전체 내용을 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상담 내역 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @Parameter(description = "상담 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(counselingService.getById(id)));
    }

    @Operation(
        summary = "상담 등록",
        description = "새 상담 내역을 등록합니다.\n\n" +
                      "**shareScope 값:** `ALL`(전체 공개) / `PRIVATE`(교사 전용)\n\n" +
                      "**요청 예시:**\n" +
                      "```json\n" +
                      "{\n" +
                      "  \"studentId\": 1,\n" +
                      "  \"date\": \"2025-03-14\",\n" +
                      "  \"content\": \"진로 상담 진행. 이공계 희망.\",\n" +
                      "  \"nextPlan\": \"2주 후 심화 상담 예정\",\n" +
                      "  \"shareScope\": \"ALL\"\n" +
                      "}\n" +
                      "```"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 없음")
    })
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CounselingRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(counselingService.create(request, auth.getName())));
    }

    @Operation(
        summary = "상담 수정",
        description = "상담 내용·날짜·다음 계획·공개범위를 수정합니다.\n\n**작성한 교사 본인만 수정 가능합니다.**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상담 내역 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "상담 ID") @PathVariable Long id,
            @Valid @RequestBody CounselingRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(counselingService.update(id, request, auth.getName())));
    }

    @Operation(
        summary = "상담 삭제",
        description = "상담 내역을 삭제합니다.\n\n**작성한 교사 본인만 삭제 가능합니다.**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상담 내역 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @Parameter(description = "상담 ID") @PathVariable Long id,
            Authentication auth) {
        counselingService.delete(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "상담 내역이 삭제되었습니다."));
    }
}
