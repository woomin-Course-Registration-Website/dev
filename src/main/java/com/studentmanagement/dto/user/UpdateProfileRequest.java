package com.studentmanagement.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 내 프로필 수정 요청 (이름만 변경 가능) */
@Getter
public class UpdateProfileRequest {
    @NotBlank(message = "이름을 입력하세요.")
    private String name;
}
