package com.studentmanagement.dto.analytics;

/** ETL 실행 결과 요약 (적재된 행 수). */
public record EtlResult(
        int students,
        int subjects,
        int grades,
        int attendance,
        int submissions,
        int feedbacks
) {}
