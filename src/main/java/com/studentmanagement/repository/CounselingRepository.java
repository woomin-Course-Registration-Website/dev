package com.studentmanagement.repository;

import com.studentmanagement.domain.Counseling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CounselingRepository extends JpaRepository<Counseling, Long> {

    @Query("SELECT c FROM Counseling c JOIN FETCH c.student JOIN FETCH c.teacher WHERE " +
           "(:studentId IS NULL OR c.student.id = :studentId) AND " +
           "(:teacherId IS NULL OR c.teacher.id = :teacherId) AND " +
           "(:from IS NULL OR c.date >= :from) AND " +
           "(:to IS NULL OR c.date <= :to) " +
           "ORDER BY c.date DESC")
    List<Counseling> findByFilters(@Param("studentId") Long studentId,
                                   @Param("teacherId") Long teacherId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    @Query("SELECT c FROM Counseling c JOIN FETCH c.student JOIN FETCH c.teacher " +
           "WHERE c.student.id = :studentId AND c.shareScope = 'ALL' ORDER BY c.date DESC")
    List<Counseling> findPublicByStudentId(@Param("studentId") Long studentId);
}
