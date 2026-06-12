package com.studentmanagement.analytics.repository.projection;

/** 학생의 과목별 평균 점수 집계 결과 (Spring Data 인터페이스 프로젝션). */
public interface SubjectAverage {
    Long getSubjectId();
    Double getAvgScore();
}
