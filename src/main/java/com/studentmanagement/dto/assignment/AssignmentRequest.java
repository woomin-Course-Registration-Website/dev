package com.studentmanagement.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class AssignmentRequest {

    @NotBlank
    private String title;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private Integer year;

    @NotNull
    private Integer semester;
}
