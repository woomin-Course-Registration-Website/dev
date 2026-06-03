package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.user.NotificationSettingsRequest;
import com.studentmanagement.dto.user.UpdateProfileRequest;
import com.studentmanagement.dto.user.UpdateUserRequest;
import com.studentmanagement.dto.user.UserRequest;
import com.studentmanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getByEmail(auth.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication auth,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateName(auth.getName(), request.getName())));
    }

    @GetMapping("/me/notification-settings")
    public ResponseEntity<?> getNotificationSettings(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getNotificationSettings(auth.getName())));
    }

    @PutMapping("/me/notification-settings")
    public ResponseEntity<?> updateNotificationSettings(Authentication auth,
                                                        @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateNotificationSettings(auth.getName(), request)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/teachers")
    public ResponseEntity<?> getTeachers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getTeachers()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAll()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        userService.delete(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "User deleted."));
    }
}
