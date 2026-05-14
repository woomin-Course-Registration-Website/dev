package com.studentmanagement.dto.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class StudentRequest {
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    private String name;

    @NotNull(message = "학년은 필수입니다.")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
    @Max(value = 3, message = "학년은 3 이하여야 합니다.")
    private Integer grade;

    @NotNull(message = "반은 필수입니다.")
    @Min(value = 1, message = "반은 1 이상이어야 합니다.")
    @Max(value = 30, message = "반은 30 이하여야 합니다.")
    private Integer classNum;

    @NotNull(message = "번호는 필수입니다.")
    @Min(value = 1, message = "번호는 1 이상이어야 합니다.")
    @Max(value = 50, message = "번호는 50 이하여야 합니다.")
    private Integer studentNum;

    private Long userId;
}
