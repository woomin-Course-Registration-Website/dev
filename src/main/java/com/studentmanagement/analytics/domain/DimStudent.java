package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 학생 차원 (Star Schema Dimension).
 *
 * PK는 운영 DB student.id를 그대로 사용하는 자연키 → ETL 시 save()가 곧 upsert가 된다.
 */
@Entity
@Table(name = "dim_student")
@Getter @Setter @NoArgsConstructor
public class DimStudent {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(nullable = false, length = 50)
    private String name;

    private int grade;

    @Column(name = "class_num")
    private int classNum;

    @Column(name = "student_num")
    private int studentNum;

    public DimStudent(Long studentId, String name, int grade, int classNum, int studentNum) {
        this.studentId = studentId;
        this.name = name;
        this.grade = grade;
        this.classNum = classNum;
        this.studentNum = studentNum;
    }
}
