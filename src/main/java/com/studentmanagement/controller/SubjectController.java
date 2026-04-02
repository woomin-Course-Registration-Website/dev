package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.subject.SubjectRequest;
import com.studentmanagement.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 과목 관리 컨트롤러
 *
 * - 성적 입력 시 참조되는 과목 마스터 데이터를 관리합니다.
 * - 목록 조회는 모든 인증된 사용자가 가능합니다.
 * - 과목 추가는 ADMIN만 가능합니다.
 */
@Tag(name = "과목 관리", description = "과목 목록 조회(전체) 및 추가(ADMIN) API")
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Operation(
        summary = "과목 목록 조회",
        description = "등록된 전체 과목 목록을 반환합니다.\n\n성적 입력 시 `subjectId`로 사용합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getAll()));
    }

    @Operation(
        summary = "과목 추가",
        description = "새 과목을 추가합니다. **ADMIN 전용**\n\n동일한 이름의 과목이 이미 존재하면 `400`이 반환됩니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "과목명 중복"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(subjectService.create(request)));
    }
}
