package com.studentmanagement.repository;

import com.studentmanagement.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    @Query("SELECT g FROM Grade g JOIN FETCH g.subject WHERE g.student.id = :studentId " +
           "AND (:year IS NULL OR g.year = :year) " +
           "AND (:semester IS NULL OR g.semester = :semester) " +
           "AND (:subjectId IS NULL OR g.subject.id = :subjectId) " +
           "ORDER BY g.year DESC, g.semester DESC")
    List<Grade> findByStudentAndFilters(@Param("studentId") Long studentId,
                                        @Param("year") Integer year,
                                        @Param("semester") Integer semester,
                                        @Param("subjectId") Long subjectId);

    Optional<Grade> findByStudentIdAndSubjectIdAndYearAndSemester(
            Long studentId, Long subjectId, int year, int semester);

    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.subject.id = :subjectId AND g.year = :year AND g.semester = :semester")
    Double findAverageScore(@Param("subjectId") Long subjectId,
                            @Param("year") int year,
                            @Param("semester") int semester);

    @Query("SELECT SUM(g.score) FROM Grade g WHERE g.student.id = :studentId AND g.year = :year AND g.semester = :semester")
    Double findTotalScore(@Param("studentId") Long studentId,
                          @Param("year") int year,
                          @Param("semester") int semester);
}
