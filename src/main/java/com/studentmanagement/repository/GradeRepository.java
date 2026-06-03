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

    /** 특정 학생 목록 + 연도/학기 + 과목에 해당하는 성적 수 집계 */
    @Query("SELECT COUNT(g) FROM Grade g WHERE g.subject.id = :subjectId " +
           "AND g.year = :year AND g.semester = :semester " +
           "AND g.student.id IN :studentIds")
    long countBySubjectYearSemesterAndStudents(@Param("subjectId") Long subjectId,
                                               @Param("year") int year,
                                               @Param("semester") int semester,
                                               @Param("studentIds") List<Long> studentIds);

    /** 반/과목별 성적 일괄 조회 (성적 입력 화면). grade/classNum/year/semester는 null이면 무시. */
    @Query("SELECT g FROM Grade g JOIN FETCH g.student JOIN FETCH g.subject " +
           "WHERE g.subject.id = :subjectId " +
           "AND (:grade IS NULL OR g.student.grade = :grade) " +
           "AND (:classNum IS NULL OR g.student.classNum = :classNum) " +
           "AND (:year IS NULL OR g.year = :year) " +
           "AND (:semester IS NULL OR g.semester = :semester) " +
           "ORDER BY g.student.studentNum ASC")
    List<Grade> findByClassAndSubject(@Param("subjectId") Long subjectId,
                                      @Param("grade") Integer grade,
                                      @Param("classNum") Integer classNum,
                                      @Param("year") Integer year,
                                      @Param("semester") Integer semester);
}
