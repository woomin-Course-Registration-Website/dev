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

    /**
     * 여러 (과목·연도·학기) 조합의 평균 점수를 한 번에 집계.
     * GROUP BY 결과에는 실제로 데이터가 존재하는 조합만 포함되므로 IN의 카디시안 곱이 결과에 영향 없음.
     * 행: [subjectId, year, semester, avgScore]
     */
    @Query("SELECT g.subject.id, g.year, g.semester, AVG(g.score) FROM Grade g " +
           "WHERE g.subject.id IN :subjectIds AND g.year IN :years AND g.semester IN :semesters " +
           "GROUP BY g.subject.id, g.year, g.semester")
    List<Object[]> findAverageScoresGrouped(@Param("subjectIds") List<Long> subjectIds,
                                            @Param("years") List<Integer> years,
                                            @Param("semesters") List<Integer> semesters);

    /**
     * 특정 학생의 (연도·학기)별 학기 총점을 한 번에 집계.
     * 행: [year, semester, totalScore]
     */
    @Query("SELECT g.year, g.semester, SUM(g.score) FROM Grade g " +
           "WHERE g.student.id = :studentId AND g.year IN :years AND g.semester IN :semesters " +
           "GROUP BY g.year, g.semester")
    List<Object[]> findTotalScoresForStudent(@Param("studentId") Long studentId,
                                             @Param("years") List<Integer> years,
                                             @Param("semesters") List<Integer> semesters);

    /** 특정 학생 목록 + 연도/학기 + 과목에 해당하는 성적 수 집계 */
    @Query("SELECT COUNT(g) FROM Grade g WHERE g.subject.id = :subjectId " +
           "AND g.year = :year AND g.semester = :semester " +
           "AND g.student.id IN :studentIds")
    long countBySubjectYearSemesterAndStudents(@Param("subjectId") Long subjectId,
                                               @Param("year") int year,
                                               @Param("semester") int semester,
                                               @Param("studentIds") List<Long> studentIds);

    /**
     * 학생 ID 집합·연도·학기 기준으로 과목별 성적 입력 수를 한 번에 집계.
     * 결과: [subjectId, count]. 데이터가 없는 과목은 결과에 포함되지 않으므로
     * 호출 측에서 0으로 채워야 한다.
     */
    @Query("SELECT g.subject.id, COUNT(g) FROM Grade g " +
           "WHERE g.year = :year AND g.semester = :semester " +
           "AND g.student.id IN :studentIds " +
           "GROUP BY g.subject.id")
    List<Object[]> countByYearSemesterGroupedBySubject(@Param("year") int year,
                                                       @Param("semester") int semester,
                                                       @Param("studentIds") List<Long> studentIds);
}
