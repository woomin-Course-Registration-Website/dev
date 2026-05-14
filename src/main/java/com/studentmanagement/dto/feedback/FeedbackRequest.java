package com.studentmanagement.dto.feedback;

import com.studentmanagement.domain.Feedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class FeedbackRequest {
    @NotNull(message = "분류는 필수입니다.")
    private Feedback.Category category;

    @NotBlank(message = "피드백 내용은 필수입니다.")
    @Size(max = 2000, message = "피드백 내용은 2000자 이내여야 합니다.")
    private String content;

    private boolean isPublic;
}
