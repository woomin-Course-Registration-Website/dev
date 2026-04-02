package com.studentmanagement.dto.auth;

import com.studentmanagement.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 회원가입 요청 DTO
 *
 * role은 TEACHER / STUDENT / PARENT 중 하나를 입력합니다.
 * ADMIN 계정은 회원가입으로 생성할 수 없으며, 기존 ADMIN이 /api/users 를 통해 생성해야 합니다.
 */
@Getter
public class RegisterRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "역할은 필수입니다.")
    private User.Role role;
}
