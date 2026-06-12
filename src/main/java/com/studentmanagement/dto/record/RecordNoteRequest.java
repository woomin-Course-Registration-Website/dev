package com.studentmanagement.dto.record;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RecordNoteRequest {
    @NotBlank
    private String content;
}
