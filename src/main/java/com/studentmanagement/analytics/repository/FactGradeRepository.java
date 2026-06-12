package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.FactGrade;
import com.studentmanagement.analytics.repository.projection.SubjectAverage;
import com.studentmanagement.analytics.repository.projection.TermAverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FactGradeRepository extends JpaRepository<FactGrade, Long> {

    /** ETL upsert 기준: 자연키로 기존 사실 조회 */
    Optional<FactGrade> findByStudentIdAndSubjectIdAndYearAndSemester(
            Long studentId, Long subjectId, int year, int semester);

    /** 학생별 학년도/학기 평균 점수 추이 */
    @Query("SELECT f.year AS year, f.semester AS semester, AVG(f.score) AS avgScore " +
           "FROM FactGrade f WHERE f.studentId = :studentId " +
           "GROUP BY f.year, f.semester ORDER BY f.year, f.semester")
    List<TermAverage> findStudentTermAverages(@Param("studentId") Long studentId);

    /** 학생별 과목 평균 점수 (어느 과목이 약한지 분석용) */
    @Query("SELECT f.subjectId AS subjectId, AVG(f.score) AS avgScore " +
           "FROM FactGrade f WHERE f.studentId = :studentId GROUP BY f.subjectId")
    List<SubjectAverage> findStudentSubjectAverages(@Param("studentId") Long studentId);

    /** 과목 전체 평균 */
    @Query("SELECT AVG(f.score) FROM FactGrade f WHERE f.subjectId = :subjectId")
    Double findSubjectAverage(@Param("subjectId") Long subjectId);

    /** 과목 점수 목록 (서비스에서 분포 버킷 계산) */
    @Query("SELECT f.score FROM FactGrade f WHERE f.subjectId = :subjectId")
    List<BigDecimal> findScoresBySubjectId(@Param("subjectId") Long subjectId);
}
