package com.studentmanagement.dto.record;

import lombok.Getter;

@Getter
public class StudentRecordRequest {
    private AttendanceDto attendance;
    private String specialNotes;

    @Getter
    public static class AttendanceDto {
        private int present;
        private int absent;
        private int late;
    }
}
