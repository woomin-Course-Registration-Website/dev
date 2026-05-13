package com.studentmanagement.util;

import com.studentmanagement.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtUtil 단위 테스트
 *
 * 토큰 발급/검증 로직을 실제 jjwt 라이브러리와 함께 검증한다.
 * 컨트롤러 테스트에서는 JwtUtil이 모킹되므로, 이 테스트가 토큰 처리 로직의 유일한 안전망이다.
 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-12345";

    private JwtUtil jwtUtil;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(SECRET);
        jwtConfig.setAccessTokenExpiration(900_000L);   // 15분
        jwtConfig.setRefreshTokenExpiration(604_800_000L); // 7일
        jwtUtil = new JwtUtil(jwtConfig);
    }

    @Test
    void generateAccessToken_includesUserClaims() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", "TEACHER");

        assertThat(jwtUtil.getEmail(token)).isEqualTo("user@test.com");
        assertThat(jwtUtil.getRole(token)).isEqualTo("TEACHER");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void generateRefreshToken_alsoIncludesClaims() {
        String token = jwtUtil.generateRefreshToken(42L, "parent@test.com", "PARENT");

        assertThat(jwtUtil.getEmail(token)).isEqualTo("parent@test.com");
        assertThat(jwtUtil.getRole(token)).isEqualTo("PARENT");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void isTokenValid_returnsTrueForFreshToken() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", "TEACHER");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        // 만료 시간을 음수로 두어 발급 즉시 만료된 토큰을 생성
        jwtConfig.setAccessTokenExpiration(-1000L);
        JwtUtil expiringUtil = new JwtUtil(jwtConfig);
        String expired = expiringUtil.generateAccessToken(1L, "user@test.com", "TEACHER");

        assertThat(expiringUtil.isTokenValid(expired)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", "TEACHER");
        // payload 부분을 변조 (마지막 글자 한 자 변경)
        String tampered = token.substring(0, token.length() - 1) +
                (token.charAt(token.length() - 1) == 'A' ? 'B' : 'A');

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithDifferentKey() {
        // 다른 secret으로 서명된 토큰을 발급
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "other-secret-key-must-be-at-least-256-bits-long-9999".getBytes(StandardCharsets.UTF_8));
        String foreign = Jwts.builder()
                .subject("attacker@test.com")
                .claim("userId", 1L)
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000L))
                .signWith(otherKey)
                .compact();

        assertThat(jwtUtil.isTokenValid(foreign)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForMalformedToken() {
        assertThat(jwtUtil.isTokenValid("not.a.jwt")).isFalse();
        assertThat(jwtUtil.isTokenValid("")).isFalse();
        assertThat(jwtUtil.isTokenValid("Bearer abc")).isFalse();
    }

    @Test
    void parseClaims_returnsExpirationAfterIssuedAt() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", "TEACHER");
        Claims claims = jwtUtil.parseClaims(token);

        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void accessAndRefreshTokens_haveDifferentExpirations() {
        String access = jwtUtil.generateAccessToken(1L, "user@test.com", "TEACHER");
        String refresh = jwtUtil.generateRefreshToken(1L, "user@test.com", "TEACHER");

        Date accessExp = jwtUtil.parseClaims(access).getExpiration();
        Date refreshExp = jwtUtil.parseClaims(refresh).getExpiration();

        assertThat(refreshExp).isAfter(accessExp);
    }
}
