package com.studentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.auth.*;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.service.AuthService;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AuthService authService;
    @MockBean  JwtUtil jwtUtil;

    // ── register ──────────────────────────────────────────────────────

    @Test
    void register_returns201() throws Exception {
        UserResponse resp = new UserResponse(TestFixtures.teacherUser());
        given(authService.register(any())).willReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"new@test.com","password":"password1","name":"신규","role":"TEACHER"}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void register_whenInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"not-an-email","password":"password1","name":"신규","role":"TEACHER"}
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_whenAdminRole_returns400() throws Exception {
        given(authService.register(any())).willThrow(new IllegalArgumentException("ADMIN"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"admin@test.com","password":"password1","name":"관리자","role":"ADMIN"}
                    """))
                .andExpect(status().isBadRequest());
    }

    // ── login ─────────────────────────────────────────────────────────

    @Test
    void login_returns200WithTokens() throws Exception {
        LoginResponse resp = new LoginResponse("access", "refresh", TestFixtures.teacherUser());
        given(authService.login(any())).willReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"teacher@test.com","password":"password1"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void login_whenWrongPassword_returns403() throws Exception {
        given(authService.login(any())).willThrow(new UnauthorizedException("비밀번호 불일치"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"teacher@test.com","password":"wrong"}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── refresh ───────────────────────────────────────────────────────

    @Test
    void refresh_returns200() throws Exception {
        LoginResponse resp = new LoginResponse("new_access", "new_refresh", TestFixtures.teacherUser());
        given(authService.refresh(any())).willReturn(resp);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"valid_refresh"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_whenInvalidToken_returns403() throws Exception {
        given(authService.refresh(any())).willThrow(new UnauthorizedException("토큰 만료"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"bad_token"}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── reset-password ────────────────────────────────────────────────

    @Test
    void resetPassword_returns200() throws Exception {
        willDoNothing().given(authService).sendResetPasswordEmail(anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"teacher@test.com"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_whenNotFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("사용자 없음"))
                .given(authService).sendResetPasswordEmail(anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"unknown@test.com"}
                    """))
                .andExpect(status().isNotFound());
    }

    // ── change-password ───────────────────────────────────────────────

    @Test
    void changePassword_whenAuthenticated_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(authService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"old_password","newPassword":"new_password123"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_whenNoToken_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"old_password","newPassword":"new_password123"}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── logout ────────────────────────────────────────────────────────

    @Test
    void logout_returns200_noAuthRequired() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }
}
