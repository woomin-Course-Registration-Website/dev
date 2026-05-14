package com.studentmanagement.dto.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class StudentRecordRequest {
    @Valid
    private AttendanceDto attendance;

    @Size(max = 2000, message = "특기사항은 2000자 이내여야 합니다.")
    private String specialNotes;

    @Getter
    public static class AttendanceDto {
        @Min(value = 0, message = "출석 일수는 0 이상이어야 합니다.")
        private int present;

        @Min(value = 0, message = "결석 일수는 0 이상이어야 합니다.")
        private int absent;

        @Min(value = 0, message = "지각 횟수는 0 이상이어야 합니다.")
        private int late;
    }
}
