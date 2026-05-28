package com.studentmanagement.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studentmanagement.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRequest {
    @NotBlank
    private String name;

    @NotNull
    private User.Role role;

    @Size(min = 8)
    private String password;
}
