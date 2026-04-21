package com.studentmanagement.service;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.user.UserRequest;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = TestFixtures.teacherUser();
    }

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getAll_returnsMappedResponses() {
        given(userRepository.findAll()).willReturn(List.of(teacher));

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("teacher@test.com");
    }

    // ── getByEmail ────────────────────────────────────────────────────

    @Test
    void getByEmail_whenExists_returnsUserResponse() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));

        UserResponse result = userService.getByEmail("teacher@test.com");

        assertThat(result.getName()).isEqualTo("김교사");
    }

    @Test
    void getByEmail_whenNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("unknown@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateName ────────────────────────────────────────────────────

    @Test
    void updateName_whenUserExists_updatesNameAndReturns() {
        given(userRepository.findByEmail("teacher@test.com")).willReturn(Optional.of(teacher));

        UserResponse result = userService.updateName("teacher@test.com", "새이름");

        assertThat(result.getName()).isEqualTo("새이름");
        assertThat(teacher.getName()).isEqualTo("새이름");
    }

    @Test
    void updateName_whenUserNotFound_throwsResourceNotFoundException() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateName("unknown@test.com", "이름"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_whenValidEmail_encodesPasswordAndSaves() {
        UserRequest req = userRequest("new@test.com", "plain", "신규", User.Role.TEACHER);
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("plain")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            TestFixtures.setId(u, 99L);
            return u;
        });

        UserResponse result = userService.create(req);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
        verify(passwordEncoder).encode("plain");
    }

    @Test
    void create_whenDuplicateEmail_throwsIllegalArgumentException() {
        UserRequest req = userRequest("teacher@test.com", "pw", "교사", User.Role.TEACHER);
        given(userRepository.existsByEmail("teacher@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_whenPasswordProvided_encodesAndUpdates() {
        given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
        given(passwordEncoder.encode("new_pw")).willReturn("new_encoded");
        UserRequest req = userRequest("teacher@test.com", "new_pw", "새이름", User.Role.TEACHER);

        UserResponse result = userService.update(1L, req);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("new_pw");
        assertThat(teacher.getPassword()).isEqualTo("new_encoded");
    }

    @Test
    void update_whenPasswordBlank_doesNotChangePassword() {
        given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
        UserRequest req = userRequest("teacher@test.com", "", "새이름", User.Role.TEACHER);

        userService.update(1L, req);

        verify(passwordEncoder, never()).encode(anyString());
        assertThat(teacher.getPassword()).isEqualTo("encoded_pw");
    }

    @Test
    void update_whenUserNotFound_throwsResourceNotFoundException() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(999L, userRequest("e@test.com", "pw", "이름", User.Role.TEACHER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_whenExists_succeeds() {
        given(userRepository.existsById(1L)).willReturn(true);

        assertThatCode(() -> userService.delete(1L)).doesNotThrowAnyException();
        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_whenNotFound_throwsResourceNotFoundException() {
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private UserRequest userRequest(String email, String password, String name, User.Role role) {
        UserRequest r = new UserRequest();
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
