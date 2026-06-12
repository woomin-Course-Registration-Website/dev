package com.studentmanagement.dto.analytics;

import java.util.List;

/** 분석 대시보드 진입용 개요 (US-08-05) — 분석 DB에 적재된 학생·과목 목록. */
public record AnalyticsOverviewResponse(
        List<StudentRef> students,
        List<SubjectRef> subjects
) {
    public record StudentRef(Long id, String name, int grade, int classNum) {}

    public record SubjectRef(Long id, String name) {}
}
