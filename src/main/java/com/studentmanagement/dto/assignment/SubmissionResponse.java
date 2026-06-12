package com.studentmanagement.dto.assignment;

import com.studentmanagement.domain.Submission;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SubmissionResponse {
    private final Long id;
    private final Long assignmentId;
    private final Long studentId;
    private final String studentName;
    private final String status;
    private final LocalDateTime submittedAt;

    public SubmissionResponse(Submission s) {
        this.id = s.getId();
        this.assignmentId = s.getAssignment().getId();
        this.studentId = s.getStudent().getId();
        this.studentName = s.getStudent().getName();
        this.status = s.getStatus().name();
        this.submittedAt = s.getSubmittedAt();
    }
}
