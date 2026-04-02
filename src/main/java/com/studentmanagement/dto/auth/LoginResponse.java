package com.studentmanagement.dto.auth;

import com.studentmanagement.domain.User;
import lombok.Getter;

@Getter
public class LoginResponse {
    private final String accessToken;
    private final String refreshToken;
    private final UserInfo user;

    public LoginResponse(String accessToken, String refreshToken, User user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = new UserInfo(user.getId(), user.getName(), user.getRole().name());
    }

    @Getter
    public static class UserInfo {
        private final Long id;
        private final String name;
        private final String role;

        public UserInfo(Long id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }
    }
}
