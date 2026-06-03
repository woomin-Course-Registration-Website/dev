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

    /** shareScope=SELECTED일 때 공유 대상 교사 ID 목록 */
    private java.util.List<Long> sharedTeacherIds = new java.util.ArrayList<>();
}
