package com.studentmanagement.dto.analytics;

import java.util.List;

/** 학생별 학습 현황 집계 (US-08-03). */
public record StudentSummaryResponse(
        Long studentId,
        String name,
        List<TermPoint> gradeTrend,
        Attendance attendance,
        SubmissionRate submission,
        List<CategoryCount> feedbackByCategory
) {
    /** 학년도/학기별 평균 점수 추이 한 점 */
    public record TermPoint(int year, int semester, double avgScore) {}

    /** 출결 요약 + 출석률 */
    public record Attendance(int present, int absent, int late, double rate) {}

    /** 과제 제출률 (SUBMITTED+LATE / 전체) */
    public record SubmissionRate(long total, long submitted, double rate) {}

    /** 피드백 카테고리별 건수 */
    public record CategoryCount(String category, long count) {}
}
