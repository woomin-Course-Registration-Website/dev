package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.FactSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface FactSubmissionRepository extends JpaRepository<FactSubmission, Long> {

    /** ETL upsert 기준: 자연키로 기존 사실 조회 */
    Optional<FactSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    long countByStudentId(Long studentId);

    long countByStudentIdAndStatusIn(Long studentId, Collection<String> statuses);

    long countBySubjectId(Long subjectId);

    long countBySubjectIdAndStatusIn(Long subjectId, Collection<String> statuses);
}
