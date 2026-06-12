package com.studentmanagement.dto.counseling;

import com.studentmanagement.domain.Counseling;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CounselingRequest {
    @NotNull(message = "학생 ID는 필수입니다.")
    private Long studentId;

    /** 진행한 상담을 기록하는 용도이므로 미래 날짜는 허용하지 않는다. */
    @NotNull(message = "상담 날짜는 필수입니다.")
    @PastOrPresent(message = "상담 날짜는 오늘 이전이어야 합니다.")
    private LocalDate date;

    @NotBlank(message = "상담 내용은 필수입니다.")
    @Size(max = 2000, message = "상담 내용은 2000자 이내여야 합니다.")
    private String content;

    @Size(max = 1000, message = "다음 상담 계획은 1000자 이내여야 합니다.")
    private String nextPlan;

    // null이면 "미지정" — 생성 시 ALL로 기본 적용, 수정 시 기존 값 유지 (누락으로 인한 의도치 않은 강등 방지)
    private Counseling.ShareScope shareScope = null;

    /** shareScope=SELECTED일 때 공유 대상 교사 ID 목록 */
    private java.util.List<Long> sharedTeacherIds = new java.util.ArrayList<>();
}
