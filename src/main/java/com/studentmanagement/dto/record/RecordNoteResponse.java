package com.studentmanagement.dto.record;

import com.studentmanagement.domain.StudentRecordNote;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecordNoteResponse {
    private final Long id;
    private final String content;
    private final LocalDateTime createdAt;

    public RecordNoteResponse(StudentRecordNote note) {
        this.id = note.getId();
        this.content = note.getContent();
        this.createdAt = note.getCreatedAt();
    }
}
