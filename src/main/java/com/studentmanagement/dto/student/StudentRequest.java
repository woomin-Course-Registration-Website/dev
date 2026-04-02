package com.studentmanagement.dto.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StudentRequest {
    @NotBlank
    private String name;

    @NotNull @Min(1) @Max(3)
    private Integer grade;

    @NotNull @Min(1)
    private Integer classNum;

    @NotNull @Min(1)
    private Integer studentNum;

    private Long userId;
}
