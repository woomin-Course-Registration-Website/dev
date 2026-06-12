package com.studentmanagement.repository;

import com.studentmanagement.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("SELECT a FROM Assignment a JOIN FETCH a.subject WHERE a.subject.id = :subjectId " +
           "ORDER BY a.dueDate DESC")
    List<Assignment> findBySubjectId(@Param("subjectId") Long subjectId);

    /** ETL 증분 적재용: updatedAt 이후 변경된 과제 */
    @Query("SELECT a FROM Assignment a JOIN FETCH a.subject WHERE a.updatedAt > :since")
    List<Assignment> findUpdatedSince(@Param("since") LocalDateTime since);
}
