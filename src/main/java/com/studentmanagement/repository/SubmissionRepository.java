package com.studentmanagement.repository;

import com.studentmanagement.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("SELECT s FROM Submission s JOIN FETCH s.student WHERE s.assignment.id = :assignmentId")
    List<Submission> findByAssignmentId(@Param("assignmentId") Long assignmentId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    /** ETL 증분 적재용: updatedAt 이후 변경된 제출 (연관 fetch 포함) */
    @Query("SELECT s FROM Submission s " +
           "JOIN FETCH s.assignment a JOIN FETCH a.subject JOIN FETCH s.student " +
           "WHERE s.updatedAt > :since")
    List<Submission> findUpdatedSince(@Param("since") LocalDateTime since);
}
