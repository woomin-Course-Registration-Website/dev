package com.studentmanagement.dto.grade;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class GradeRequest {
    @NotNull
    private Long subjectId;

    @NotNull @Min(2000) @Max(2100)
    private Integer year;

    @NotNull @Min(1) @Max(2)
    private Integer semester;

    @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal score;
}
