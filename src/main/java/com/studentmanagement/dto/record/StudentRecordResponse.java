package com.studentmanagement.dto.record;

import com.studentmanagement.domain.StudentRecord;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StudentRecordResponse {
    private final Long id;
    private final Long studentId;
    private final String attendance;
    private final String specialNotes;
    private final LocalDateTime updatedAt;

    public StudentRecordResponse(StudentRecord record) {
        this.id = record.getId();
        this.studentId = record.getStudent().getId();
        this.attendance = record.getAttendance();
        this.specialNotes = record.getSpecialNotes();
        this.updatedAt = record.getUpdatedAt();
    }
}
