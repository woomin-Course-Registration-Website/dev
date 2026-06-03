package com.studentmanagement.service;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.user.NotificationSettingsRequest;
import com.studentmanagement.dto.user.NotificationSettingsResponse;
import com.studentmanagement.dto.user.TeacherOptionResponse;
import com.studentmanagement.dto.user.UpdateUserRequest;
import com.studentmanagement.dto.user.UserRequest;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::new).toList();
    }

    public List<TeacherOptionResponse> getTeachers() {
        return userRepository.findByRoleOrderByNameAsc(User.Role.TEACHER)
                .stream().map(TeacherOptionResponse::new).toList();
    }

    public NotificationSettingsResponse getNotificationSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new NotificationSettingsResponse(user);
    }

    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(String email, NotificationSettingsRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setNotifyGrade(req.isNotifyGrade());
        user.setNotifyFeedback(req.isNotifyFeedback());
        user.setNotifyCounseling(req.isNotifyCounseling());
        return new NotificationSettingsResponse(user);
    }

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new UserResponse(user);
    }

    @Transactional
    public UserResponse updateName(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setName(name);
        return new UserResponse(user);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getRole()
        );
        return new UserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        assertAdminWouldRemain(user, request.getRole());

        user.setName(request.getName());
        user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return new UserResponse(user);
    }

    @Transactional
    public void delete(Long id, String requesterEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.getEmail().equals(requesterEmail)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        assertAdminWouldRemain(user, null);
        userRepository.delete(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found.");
        }
        userRepository.deleteById(id);
    }

    private void assertAdminWouldRemain(User user, User.Role nextRole) {
        boolean removingAdmin = user.getRole() == User.Role.ADMIN
                && (nextRole == null || nextRole != User.Role.ADMIN);
        if (removingAdmin && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("At least one ADMIN account must remain.");
        }
    }
}
