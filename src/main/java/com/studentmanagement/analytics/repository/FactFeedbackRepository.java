package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.FactFeedback;
import com.studentmanagement.analytics.repository.projection.CategoryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FactFeedbackRepository extends JpaRepository<FactFeedback, Long> {

    /** ETL upsert 기준: 운영 feedback.id로 기존 사실 조회 */
    Optional<FactFeedback> findByFeedbackId(Long feedbackId);

    /** 학생별 카테고리 건수 */
    @Query("SELECT f.category AS category, COUNT(f) AS cnt " +
           "FROM FactFeedback f WHERE f.studentId = :studentId GROUP BY f.category")
    List<CategoryCount> findCategoryCounts(@Param("studentId") Long studentId);
}
