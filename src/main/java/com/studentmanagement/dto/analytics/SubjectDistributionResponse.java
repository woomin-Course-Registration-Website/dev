package com.studentmanagement.dto.analytics;

import java.util.List;

/** 과목별 학습 현황 집계 (US-08-04). */
public record SubjectDistributionResponse(
        Long subjectId,
        String name,
        double average,
        List<Bucket> distribution,
        SubmissionRate submission
) {
    /** 점수 분포 버킷 (예: "60-69") */
    public record Bucket(String range, long count) {}

    /** 과목 과제 제출률 */
    public record SubmissionRate(long total, long submitted, double rate) {}
}
