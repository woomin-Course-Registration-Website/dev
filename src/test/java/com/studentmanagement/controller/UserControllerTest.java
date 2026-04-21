package com.studentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.service.UserService;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  UserService userService;
    @MockBean  JwtUtil jwtUtil;

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getAll_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(userService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/api/users").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(get("/api/users").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_admin_returns201() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(userService.create(any())).willReturn(new UserResponse(TestFixtures.teacherUser()));

        mockMvc.perform(post("/api/users")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"new@test.com","password":"pass1234","name":"신규","role":"TEACHER"}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void create_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(post("/api/users")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"new@test.com","password":"pass1234","name":"신규","role":"TEACHER"}
                    """))
                .andExpect(status().isForbidden());
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(userService.update(anyLong(), any())).willReturn(new UserResponse(TestFixtures.teacherUser()));

        mockMvc.perform(put("/api/users/1")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"teacher@test.com","password":"pass1234","name":"수정이름","role":"TEACHER"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void update_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(put("/api/users/1")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"teacher@test.com","password":"pass1234","name":"수정이름","role":"TEACHER"}
                    """))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(userService.update(anyLong(), any())).willThrow(new ResourceNotFoundException("없음"));

        mockMvc.perform(put("/api/users/999")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"x@test.com","password":"pass1234","name":"이름","role":"TEACHER"}
                    """))
                .andExpect(status().isNotFound());
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        willDoNothing().given(userService).delete(anyLong());

        mockMvc.perform(delete("/api/users/1").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void delete_teacher_returns403() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(delete("/api/users/1").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ── getMe / updateMe ──────────────────────────────────────────────

    @Test
    void getMe_anyRole_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(userService.getByEmail(anyString())).willReturn(new UserResponse(TestFixtures.teacherUser()));

        mockMvc.perform(get("/api/users/me").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMe_anyRole_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(userService.updateName(anyString(), anyString()))
                .willReturn(new UserResponse(TestFixtures.teacherUser()));

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"새이름"}
                    """))
                .andExpect(status().isOk());
    }
}
