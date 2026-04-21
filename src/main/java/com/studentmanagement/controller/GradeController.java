package com.studentmanagement.controller;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.grade.GradeRequest;
import com.studentmanagement.dto.grade.GradeStatsItem;
import com.studentmanagement.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 성적 관리 컨트롤러
 *
 * - 학생별 성적 조회/입력/수정/삭제를 담당합니다.
 * - 성적 입력 시 gradeRank(등급)가 자동 계산되어 저장됩니다.
 * - 성적 조회 시 과목 평균(average)과 학기 총점(total)이 함께 반환됩니다.
 *
 * URL 구조:
 *   - /api/students/{studentId}/grades  → 학생별 성적 목록/입력
 *   - /api/grades/{id}                  → 개별 성적 수정/삭제
 */
@Tag(name = "성적 관리", description = "학생 성적 입력·조회·수정·삭제 API")
@RestController
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Operation(
        summary = "성적 목록 조회",
        description = "특정 학생의 성적 목록을 반환합니다.\n\n" +
                      "**필터 파라미터 (모두 선택)**\n" +
                      "- `year`: 연도 (예: 2025)\n" +
                      "- `semester`: 학기 (1 또는 2)\n" +
                      "- `subjectId`: 과목 ID\n\n" +
                      "각 성적에는 해당 과목의 전체 평균(`average`)과 해당 학생의 학기 총점(`total`)이 포함됩니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/api/students/{studentId}/grades")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getGrades(
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @Parameter(description = "연도 필터 (예: 2025)") @RequestParam(required = false) Integer year,
            @Parameter(description = "학기 필터 (1 또는 2)") @RequestParam(required = false) Integer semester,
            @Parameter(description = "과목 ID 필터") @RequestParam(required = false) Long subjectId,
            Authentication auth) {
        User.Role role = User.Role.valueOf(
                auth.getAuthorities().stream().findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElseThrow());
        return ResponseEntity.ok(ApiResponse.ok(
                gradeService.getGrades(studentId, year, semester, subjectId, auth.getName(), role)));
    }

    @Operation(
        summary = "성적 입력",
        description = "학생의 성적을 입력합니다.\n\n" +
                      "- 동일한 학생·과목·연도·학기 조합이 이미 존재하면 `409 Conflict`가 반환됩니다.\n" +
                      "- `gradeRank`는 점수에 따라 자동 계산됩니다: 90↑ A / 80↑ B / 70↑ C / 60↑ D / 60↓ F"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "입력 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 또는 과목 없음")
    })
    @PostMapping("/api/students/{studentId}/grades")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> create(
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(gradeService.create(studentId, request)));
    }

    @Operation(summary = "성적 수정", description = "점수를 수정합니다. gradeRank는 새 점수로 자동 재계산됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성적 없음")
    })
    @PutMapping("/api/grades/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> update(
            @Parameter(description = "성적 ID") @PathVariable Long id,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(gradeService.update(id, request)));
    }

    @Operation(summary = "성적 삭제", description = "성적 레코드를 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성적 없음")
    })
    @DeleteMapping("/api/grades/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> delete(
            @Parameter(description = "성적 ID") @PathVariable Long id) {
        gradeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "성적이 삭제되었습니다."));
    }

    @Operation(summary = "과목별 성적 입력 현황",
               description = "대시보드용. 과목별 성적 입력 학생 수 / 전체 학생 수를 반환합니다.")
    @GetMapping("/api/grades/stats")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<GradeStatsItem>>> getStats(
            @Parameter(description = "학년") @RequestParam(required = false) Integer grade,
            @Parameter(description = "반")  @RequestParam(required = false) Integer classNum,
            @Parameter(description = "연도") @RequestParam(required = false) Integer year,
            @Parameter(description = "학기") @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(gradeService.getStats(grade, classNum, year, semester)));
    }
}
