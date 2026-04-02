package com.studentmanagement.dto.grade;

import com.studentmanagement.domain.Grade;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class GradeResponse {
    private final Long id;
    private final SubjectInfo subject;
    private final int year;
    private final int semester;
    private final BigDecimal score;
    private final String gradeRank;
    private final Double average;
    private final Double total;

    public GradeResponse(Grade grade, Double average, Double total) {
        this.id = grade.getId();
        this.subject = new SubjectInfo(grade.getSubject().getId(), grade.getSubject().getName());
        this.year = grade.getYear();
        this.semester = grade.getSemester();
        this.score = grade.getScore();
        this.gradeRank = grade.getGradeRank();
        this.average = average;
        this.total = total;
    }

    @Getter
    public static class SubjectInfo {
        private final Long id;
        private final String name;

        public SubjectInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
