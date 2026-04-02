package com.studentmanagement.repository;

import com.studentmanagement.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f JOIN FETCH f.teacher WHERE f.student.id = :studentId " +
           "ORDER BY f.createdAt DESC")
    List<Feedback> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Long studentId);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.teacher WHERE f.student.id = :studentId " +
           "AND f.isPublic = true ORDER BY f.createdAt DESC")
    List<Feedback> findPublicByStudentId(@Param("studentId") Long studentId);
}
