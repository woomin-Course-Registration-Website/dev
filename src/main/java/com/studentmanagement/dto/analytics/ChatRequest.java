package com.studentmanagement.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatRequest {

    /** 사용자 질의 */
    @NotBlank
    private String message;

    /** 특정 학생 한정 질의(선택). null이면 전체 개요·과목 분포를 컨텍스트로 사용. */
    private Long studentId;
}
