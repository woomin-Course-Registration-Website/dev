package com.studentmanagement.service;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.auth.LoginRequest;
import com.studentmanagement.dto.auth.LoginResponse;
import com.studentmanagement.dto.auth.RefreshRequest;
import com.studentmanagement.dto.auth.RegisterRequest;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.repository.UserRepository;
import com.studentmanagement.util.JwtUtil;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 인증 서비스
 *
 * 로그인, 토큰 재발급, 비밀번호 재설정 비즈니스 로직을 처리합니다.
 * JWT는 Stateless 방식으로 관리되며, 서버 측 세션이나 토큰 블랙리스트는 사용하지 않습니다.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
    }

    /**
     * 회원가입 처리
     *
     * 이메일 중복 여부를 확인한 후 새 사용자 계정을 생성합니다.
     * 비밀번호는 BCrypt로 암호화하여 저장합니다.
     * ADMIN 역할은 보안상 회원가입으로 생성할 수 없습니다.
     *
     * @throws IllegalArgumentException 이메일 중복 또는 ADMIN 역할 시도 시
     */
    public UserResponse register(RegisterRequest request) {
        if (request.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN 계정은 회원가입으로 생성할 수 없습니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getRole()
        );
        return new com.studentmanagement.dto.user.UserResponse(userRepository.save(user));
    }

    /**
     * 로그인 처리
     *
     * 이메일로 사용자를 조회하고 BCrypt로 비밀번호를 검증합니다.
     * 성공 시 accessToken(15분)과 refreshToken(7일)을 발급합니다.
     * 보안상 "이메일 없음"과 "비밀번호 불일치"를 동일한 오류 메시지로 처리합니다.
     *
     * @throws UnauthorizedException 이메일 또는 비밀번호 불일치
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(accessToken, refreshToken, user);
    }

    /**
     * Access Token 재발급
     *
     * 유효한 Refresh Token을 검증한 후 새 Access Token과 Refresh Token을 발급합니다.
     * Refresh Token Rotation 방식을 적용하여 매 재발급 시 Refresh Token도 갱신됩니다.
     *
     * @throws UnauthorizedException 유효하지 않은 Refresh Token
     */
    public LoginResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();
        if (!jwtUtil.isTokenValid(token)) {
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }

        String email = jwtUtil.getEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(accessToken, refreshToken, user);
    }

    /**
     * 비밀번호 재설정 이메일 발송
     *
     * UUID 기반 8자리 임시 비밀번호를 생성하여 DB에 저장하고 이메일로 발송합니다.
     * 임시 비밀번호는 BCrypt로 암호화되어 저장됩니다.
     *
     * @throws ResourceNotFoundException 등록된 이메일 없음
     */
    /**
     * 비밀번호 변경 (로그인 상태에서)
     *
     * @throws UnauthorizedException 현재 비밀번호 불일치
     */
    @org.springframework.transaction.annotation.Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UnauthorizedException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void sendResetPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 이메일로 등록된 사용자가 없습니다."));

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[학생관리시스템] 임시 비밀번호 안내");
        message.setText("임시 비밀번호: " + tempPassword + "\n로그인 후 비밀번호를 변경해주세요.");
        mailSender.send(message);

        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
    }
}
