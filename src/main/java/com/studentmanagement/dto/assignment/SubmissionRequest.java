package com.studentmanagement.dto.assignment;

import com.studentmanagement.domain.Submission;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SubmissionRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Submission.Status status;
}
