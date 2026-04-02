package com.studentmanagement.dto.counseling;

import com.studentmanagement.domain.Counseling;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class CounselingResponse {
    private final Long id;
    private final ParticipantInfo teacher;
    private final ParticipantInfo student;
    private final LocalDate date;
    private final String content;
    private final String nextPlan;
    private final String shareScope;
    private final LocalDateTime createdAt;

    public CounselingResponse(Counseling c) {
        this.id = c.getId();
        this.teacher = new ParticipantInfo(c.getTeacher().getId(), c.getTeacher().getName());
        this.student = new ParticipantInfo(c.getStudent().getId(), c.getStudent().getName());
        this.date = c.getDate();
        this.content = c.getContent();
        this.nextPlan = c.getNextPlan();
        this.shareScope = c.getShareScope().name();
        this.createdAt = c.getCreatedAt();
    }

    @Getter
    public static class ParticipantInfo {
        private final Long id;
        private final String name;

        public ParticipantInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
