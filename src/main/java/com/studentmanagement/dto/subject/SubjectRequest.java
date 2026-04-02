package com.studentmanagement.dto.subject;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SubjectRequest {
    @NotBlank
    private String name;
}
