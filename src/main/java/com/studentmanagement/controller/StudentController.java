package com.studentmanagement.controller;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.student.ParentLinkRequest;
import com.studentmanagement.dto.student.StudentRequest;
import com.studentmanagement.service.CounselingService;
import com.studentmanagement.service.StudentService;
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
 * 학생 정보 관리 컨트롤러
 *
 * - 목록/상세 조회, 등록, 정보 수정 기능을 제공합니다.
 * - 학생 삭제는 지원하지 않습니다 (연관 데이터 보존).
 * - STUDENT/PARENT는 상세 조회만 가능합니다 (본인 또는 자녀).
 */
@Tag(name = "학생 관리", description = "학생 등록·조회·수정 API")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final CounselingService counselingService;

    public StudentController(StudentService studentService, CounselingService counselingService) {
        this.studentService = studentService;
        this.counselingService = counselingService;
    }

    @Operation(
        summary = "학생 목록 조회",
        description = "전체 학생 목록을 반환합니다. 학년·반·이름으로 필터링 가능합니다.\n\n" +
                      "- `grade`: 학년 필터 (1~3)\n" +
                      "- `classNum`: 반 필터\n" +
                      "- `keyword`: 이름 검색 (부분 일치)"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "TEACHER 권한 없음")
    })
    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> getAll(
            @Parameter(description = "학년 필터 (1~3)") @RequestParam(required = false) Integer grade,
            @Parameter(description = "반 필터") @RequestParam(required = false) Integer classNum,
            @Parameter(description = "이름 검색 키워드") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getAll(grade, classNum, keyword)));
    }

    @Operation(summary = "내 학생 정보 조회", description = "현재 로그인한 STUDENT 계정에 연동된 학생 정보를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "연동된 학생 없음")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyStudent(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getMyStudent(auth.getName())));
    }

    @Operation(summary = "학생 상세 조회", description = "특정 학생의 상세 정보를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 없음")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getById(
            @Parameter(description = "학생 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getById(id)));
    }

    @Operation(
        summary = "학생 등록",
        description = "새 학생을 등록합니다. `userId`는 선택값으로, 학생 계정과 연동할 때 입력합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "TEACHER 권한 없음")
    })
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(studentService.create(request)));
    }

    @Operation(summary = "학생 정보 수정", description = "학년·반·번호·이름 등 학생 기본 정보를 수정합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 없음")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> update(
            @Parameter(description = "학생 ID") @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.update(id, request)));
    }

    @Operation(summary = "내 자녀 목록 조회", description = "현재 로그인한 PARENT 계정에 연동된 자녀 학생 목록을 반환합니다.")
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<?> getMyChildren(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getMyChildren(auth.getName())));
    }

    @Operation(summary = "학부모 계정 연동", description = "학생에 PARENT 역할 계정을 연동합니다. (교사 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "연동 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PARENT 역할이 아닌 계정"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 또는 사용자 없음")
    })
    @PostMapping("/{id}/parents")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> linkParent(
            @Parameter(description = "학생 ID") @PathVariable Long id,
            @Valid @RequestBody ParentLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.linkParent(id, request.getParentUserId())));
    }

    @Operation(summary = "학부모 계정 연동 해제", description = "학생에서 학부모 계정 연동을 해제합니다. (교사 전용)")
    @DeleteMapping("/{id}/parents/{parentUserId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> unlinkParent(
            @Parameter(description = "학생 ID") @PathVariable Long id,
            @Parameter(description = "학부모 User ID") @PathVariable Long parentUserId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.unlinkParent(id, parentUserId)));
    }

    @Operation(summary = "학생/학부모용 공개 상담 조회",
               description = "STUDENT/PARENT가 본인(또는 자녀)의 전체공개(shareScope=ALL) 상담만 조회합니다.")
    @GetMapping("/{id}/counselings")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    public ResponseEntity<?> getPublicCounselings(
            @Parameter(description = "학생 ID") @PathVariable Long id,
            Authentication auth) {
        User.Role role = User.Role.valueOf(
                auth.getAuthorities().stream().findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElseThrow());
        return ResponseEntity.ok(ApiResponse.ok(
                counselingService.getPublicForStudent(id, auth.getName(), role)));
    }
}
