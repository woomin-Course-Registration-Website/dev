package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.user.UpdateProfileRequest;
import com.studentmanagement.dto.user.UserRequest;
import com.studentmanagement.service.UserService;
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
 * 사용자 계정 관리 컨트롤러
 *
 * - GET/PUT /api/users/me : 인증된 사용자라면 누구나 자신의 프로필 조회·수정 가능
 * - 나머지 CRUD : ADMIN 권한 필요
 */
@Tag(name = "사용자 관리", description = "내 프로필 조회·수정 및 ADMIN 전용 계정 CRUD")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── 내 프로필 (인증된 모든 사용자) ──────────────────────────

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getByEmail(auth.getName())));
    }

    @Operation(summary = "내 프로필 수정 (이름 변경)")
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication auth,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateName(auth.getName(), request.getName())));
    }

    // ── ADMIN 전용 ───────────────────────────────────────────────

    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 계정 목록을 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAll()));
    }

    @Operation(summary = "사용자 생성", description = "새 계정을 생성합니다. role은 TEACHER / STUDENT / PARENT / ADMIN 중 하나.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 이메일 중복")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userService.create(request)));
    }

    @Operation(summary = "사용자 수정", description = "이름·역할·비밀번호를 변경합니다. 비밀번호 미입력 시 기존 유지.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "사용자 ID") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @Operation(summary = "사용자 삭제", description = "계정을 삭제합니다. 연관된 학생 레코드의 user_id는 NULL로 변경됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @Parameter(description = "사용자 ID") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "사용자가 삭제되었습니다."));
    }
}
