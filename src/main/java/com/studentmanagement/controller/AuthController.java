package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.auth.ChangePasswordRequest;
import com.studentmanagement.dto.auth.LoginRequest;
import com.studentmanagement.dto.auth.RefreshRequest;
import com.studentmanagement.dto.auth.RegisterRequest;
import com.studentmanagement.dto.auth.ResetPasswordRequest;
import com.studentmanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 * - 로그인 / 로그아웃 / 토큰 재발급 / 비밀번호 재설정
 * - /api/auth/** 경로는 JWT 인증 없이 접근 가능
 */
@Tag(name = "Auth", description = "인증 API (로그인, 토큰 재발급, 비밀번호 재설정)")
@RestController
@RequestMapping("/api/auth")
@SecurityRequirements  // 이 컨트롤러의 모든 엔드포인트는 JWT 불필요
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
        summary = "회원가입",
        description = "새 계정을 생성합니다.\n\n" +
                      "**role 값:** `TEACHER` / `STUDENT` / `PARENT`\n\n" +
                      "> `ADMIN` 계정은 회원가입으로 생성할 수 없습니다. 기존 ADMIN이 `POST /api/users`로 생성해야 합니다.\n\n" +
                      "**요청 예시:**\n" +
                      "```json\n" +
                      "{\n" +
                      "  \"email\": \"teacher@school.kr\",\n" +
                      "  \"password\": \"password123\",\n" +
                      "  \"name\": \"김교사\",\n" +
                      "  \"role\": \"TEACHER\"\n" +
                      "}\n" +
                      "```"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 이메일 중복")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(request), "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공 — accessToken, refreshToken 반환"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "로그아웃", description = "클라이언트 측 토큰을 폐기합니다. (서버 stateless — 토큰 블랙리스트 미사용)")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(ApiResponse.ok(null, "로그아웃 되었습니다."));
    }

    @Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token으로 새 Access Token을 발급합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "유효하지 않은 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "비밀번호 재설정", description = "등록된 이메일로 임시 비밀번호를 발송합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 비밀번호 이메일 발송 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 이메일로 등록된 사용자 없음")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(null, "임시 비밀번호가 이메일로 발송되었습니다."));
    }

    @Operation(summary = "비밀번호 변경", description = "로그인 상태에서 현재 비밀번호를 확인 후 새 비밀번호로 변경합니다.")
    @PostMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> changePassword(Authentication auth,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(auth.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, "비밀번호가 변경되었습니다."));
    }
}
