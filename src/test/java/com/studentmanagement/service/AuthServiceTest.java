package com.studentmanagement.service;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.auth.*;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.UserRepository;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock JavaMailSender mailSender;

    @InjectMocks AuthService authService;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = TestFixtures.teacherUser();
    }

    // ── register ──────────────────────────────────────────────────────

    @Test
    void register_whenValidRequest_returnsUserResponse() {
        RegisterRequest req = buildRegisterRequest("new@test.com", "password1", "신규", User.Role.TEACHER);
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            TestFixtures.setId(u, 99L);
            return u;
        });

        UserResponse result = authService.register(req);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getName()).isEqualTo("신규");
    }

    @Test
    void register_whenAdminRole_throwsIllegalArgumentException() {
        RegisterRequest req = buildRegisterRequest("admin@test.com", "password1", "어드민", User.Role.ADMIN);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void register_whenDuplicateEmail_throwsIllegalArgumentException() {
        RegisterRequest req = buildRegisterRequest("teacher@test.com", "password1", "교사", User.Role.TEACHER);
        given(userRepository.existsByEmail("teacher@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_passwordIsEncoded() {
        RegisterRequest req = buildRegisterRequest("new@test.com", "plain_password", "교사", User.Role.TEACHER);
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode("plain_password")).willReturn("bcrypt_hash");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            TestFixtures.setId(u, 99L);
            return u;
        });

        authService.register(req);

        verify(passwordEncoder).encode("plain_password");
    }

    // ── login ────────────────────────────────────────────────────────

    @Test
    void login_whenValidCredentials_returnsTokenPair() {
        LoginRequest req = new LoginRequest();
        setField(req, "email", "teacher@test.com");
        setField(req, "password", "password1");

        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(passwordEncoder.matches("password1", "encoded_pw")).willReturn(true);
        given(jwtUtil.generateAccessToken(any(), any(), any())).willReturn("access_token");
        given(jwtUtil.generateRefreshToken(any(), any(), any())).willReturn("refresh_token");

        LoginResponse result = authService.login(req);

        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(result.getUser().getName()).isEqualTo("김교사");
    }

    @Test
    void login_whenEmailNotFound_throwsUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        setField(req, "email", "unknown@test.com");
        setField(req, "password", "password1");
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_whenWrongPassword_throwsUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        setField(req, "email", "teacher@test.com");
        setField(req, "password", "wrong");
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(passwordEncoder.matches("wrong", "encoded_pw")).willReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── refresh ───────────────────────────────────────────────────────

    @Test
    void refresh_whenValidToken_returnsNewTokenPair() {
        RefreshRequest req = new RefreshRequest();
        setField(req, "refreshToken", "valid_refresh");
        given(jwtUtil.isTokenValid("valid_refresh")).willReturn(true);
        given(jwtUtil.getEmail("valid_refresh")).willReturn("teacher@test.com");
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(jwtUtil.generateAccessToken(any(), any(), any())).willReturn("new_access");
        given(jwtUtil.generateRefreshToken(any(), any(), any())).willReturn("new_refresh");

        LoginResponse result = authService.refresh(req);

        assertThat(result.getAccessToken()).isEqualTo("new_access");
        assertThat(result.getRefreshToken()).isEqualTo("new_refresh");
    }

    @Test
    void refresh_whenInvalidToken_throwsUnauthorizedException() {
        RefreshRequest req = new RefreshRequest();
        setField(req, "refreshToken", "bad_token");
        given(jwtUtil.isTokenValid("bad_token")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── changePassword ────────────────────────────────────────────────

    @Test
    void changePassword_whenCurrentPasswordMatches_encodesNewPassword() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(passwordEncoder.matches("old_pw", "encoded_pw")).willReturn(true);
        given(passwordEncoder.encode("new_pw")).willReturn("new_encoded");
        given(userRepository.save(any())).willReturn(teacher);

        authService.changePassword("teacher@test.com", "old_pw", "new_pw");

        verify(passwordEncoder).encode("new_pw");
        assertThat(teacher.getPassword()).isEqualTo("new_encoded");
    }

    @Test
    void changePassword_whenCurrentPasswordWrong_throwsUnauthorizedException() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(passwordEncoder.matches("wrong", "encoded_pw")).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword("teacher@test.com", "wrong", "new_pw"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void changePassword_whenUserNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword("unknown@test.com", "pw", "new"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── sendResetPasswordEmail ────────────────────────────────────────

    @Test
    void sendResetPasswordEmail_whenEmailExists_sendsMailAndUpdatesPassword() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(passwordEncoder.encode(anyString())).willReturn("temp_encoded");
        given(userRepository.save(any())).willReturn(teacher);

        authService.sendResetPasswordEmail("teacher@test.com");

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void sendResetPasswordEmail_whenEmailNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendResetPasswordEmail("unknown@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendResetPasswordEmail_tempPasswordIsEightChars() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));
        given(userRepository.save(any())).willReturn(teacher);

        ArgumentCaptor<String> pwCaptor = ArgumentCaptor.forClass(String.class);
        given(passwordEncoder.encode(pwCaptor.capture())).willReturn("hashed");

        authService.sendResetPasswordEmail("teacher@test.com");

        assertThat(pwCaptor.getValue()).hasSize(8);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private RegisterRequest buildRegisterRequest(String email, String password, String name, User.Role role) {
        RegisterRequest r = new RegisterRequest();
        setField(r, "email", email);
        setField(r, "password", password);
        setField(r, "name", name);
        setField(r, "role", role);
        return r;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
