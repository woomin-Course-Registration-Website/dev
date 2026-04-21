package com.studentmanagement.fixture;

import com.studentmanagement.util.JwtUtil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class SecurityTestHelper {

    public static final String FAKE_TOKEN = "Bearer fake.test.token";
    public static final String RAW_TOKEN  = "fake.test.token";

    public static void stubAsTeacher(JwtUtil jwtUtil) {
        stub(jwtUtil, "TEACHER", "teacher@test.com", 1L);
    }

    public static void stubAsStudent(JwtUtil jwtUtil) {
        stub(jwtUtil, "STUDENT", "student@test.com", 2L);
    }

    public static void stubAsParent(JwtUtil jwtUtil) {
        stub(jwtUtil, "PARENT", "parent@test.com", 3L);
    }

    public static void stubAsAdmin(JwtUtil jwtUtil) {
        stub(jwtUtil, "ADMIN", "admin@test.com", 4L);
    }

    public static void stubAsInvalid(JwtUtil jwtUtil) {
        when(jwtUtil.isTokenValid(anyString())).thenReturn(false);
    }

    private static void stub(JwtUtil jwtUtil, String role, String email, Long userId) {
        when(jwtUtil.isTokenValid(RAW_TOKEN)).thenReturn(true);
        when(jwtUtil.getRole(RAW_TOKEN)).thenReturn(role);
        when(jwtUtil.getEmail(RAW_TOKEN)).thenReturn(email);
        when(jwtUtil.getUserId(RAW_TOKEN)).thenReturn(userId);
    }
}
