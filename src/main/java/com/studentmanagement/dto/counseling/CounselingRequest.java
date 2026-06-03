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

    // null이면 "미지정" — 생성 시 ALL로 기본 적용, 수정 시 기존 값 유지 (누락으로 인한 의도치 않은 강등 방지)
    private Counseling.ShareScope shareScope = null;

    /** shareScope=SELECTED일 때 공유 대상 교사 ID 목록 */
    private java.util.List<Long> sharedTeacherIds = new java.util.ArrayList<>();
}
