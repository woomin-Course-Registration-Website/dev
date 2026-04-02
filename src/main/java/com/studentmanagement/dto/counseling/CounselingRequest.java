package com.studentmanagement.dto.counseling;

import com.studentmanagement.domain.Counseling;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CounselingRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private LocalDate date;

    @NotBlank
    private String content;

    private String nextPlan;

    private Counseling.ShareScope shareScope = Counseling.ShareScope.ALL;
}
