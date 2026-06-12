package com.studentmanagement.repository;

import com.studentmanagement.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f JOIN FETCH f.teacher WHERE f.student.id = :studentId " +
           "ORDER BY f.createdAt DESC")
    List<Feedback> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Long studentId);

    /** ETL 증분 적재용: updatedAt 이후 변경된 피드백 */
    @Query("SELECT f FROM Feedback f WHERE f.updatedAt > :since")
    List<Feedback> findUpdatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.teacher WHERE f.student.id = :studentId " +
           "AND f.isPublic = true ORDER BY f.createdAt DESC")
    List<Feedback> findPublicByStudentId(@Param("studentId") Long studentId);
}
