package com.studentmanagement.dto.feedback;

import com.studentmanagement.domain.Feedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class FeedbackRequest {
    @NotNull
    private Feedback.Category category;

    @NotBlank
    private String content;

    private boolean isPublic;
}
