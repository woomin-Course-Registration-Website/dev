package com.studentmanagement.dto.feedback;

import com.studentmanagement.domain.Feedback;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FeedbackResponse {
    private final Long id;
    private final TeacherInfo teacher;
    private final Long studentId;
    private final String category;
    private final String content;
    private final boolean isPublic;
    private final LocalDateTime createdAt;

    public FeedbackResponse(Feedback f) {
        this.id = f.getId();
        this.teacher = new TeacherInfo(f.getTeacher().getId(), f.getTeacher().getName());
        this.studentId = f.getStudent().getId();
        this.category = f.getCategory().name();
        this.content = f.getContent();
        this.isPublic = f.isPublic();
        this.createdAt = f.getCreatedAt();
    }

    @Getter
    public static class TeacherInfo {
        private final Long id;
        private final String name;

        public TeacherInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
