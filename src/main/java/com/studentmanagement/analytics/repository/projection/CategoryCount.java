package com.studentmanagement.analytics.repository.projection;

/** 카테고리별 건수 집계 결과 (Spring Data 인터페이스 프로젝션). */
public interface CategoryCount {
    String getCategory();
    long getCnt();
}
