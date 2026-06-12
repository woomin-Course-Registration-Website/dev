package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 성적 사실 (Star Schema Fact).
 *
 * (student, subject, 학년도, 학기)별 점수·등급. 운영 grades 테이블에서 적재된다.
 * 자연키 (studentId, subjectId, year, semester) 유니크 → ETL upsert 기준.
 */
@Entity
@Table(name = "fact_grade",
        uniqueConstraints = @UniqueConstraint(name = "uq_fact_grade",
                columnNames = {"student_id", "subject_id", "year", "semester"}))
@Getter @Setter @NoArgsConstructor
public class FactGrade {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** dim_date 참조 키 (year*10+semester) */
    @Column(name = "date_key", nullable = false)
    private Integer dateKey;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int semester;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "grade_rank", length = 2)
    private String gradeRank;
}
