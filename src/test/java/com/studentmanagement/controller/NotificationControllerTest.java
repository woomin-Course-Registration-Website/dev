package com.studentmanagement.controller;

import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.service.NotificationService;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  NotificationService notificationService;
    @MockBean  JwtUtil jwtUtil;

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getNotifications_authenticated_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(notificationService.getMyNotifications(anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/notifications").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getNotifications_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    // ── markAsRead ────────────────────────────────────────────────────

    @Test
    void markAsRead_owner_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(notificationService).markAsRead(anyLong(), anyString());

        mockMvc.perform(put("/api/notifications/500/read").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_notOwner_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willThrow(new UnauthorizedException("권한 없음"))
                .given(notificationService).markAsRead(anyLong(), anyString());

        mockMvc.perform(put("/api/notifications/500/read").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── markAllAsRead ─────────────────────────────────────────────────

    @Test
    void markAllAsRead_authenticated_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(notificationService).markAllAsRead(anyString());

        mockMvc.perform(put("/api/notifications/read-all").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }
}
