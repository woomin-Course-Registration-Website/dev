package com.studentmanagement.analytics.repository.projection;

/** 학년도/학기별 평균 점수 집계 결과 (Spring Data 인터페이스 프로젝션). */
public interface TermAverage {
    int getYear();
    int getSemester();
    Double getAvgScore();
}
