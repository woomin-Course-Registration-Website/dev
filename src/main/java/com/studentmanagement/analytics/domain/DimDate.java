package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 학기(기간) 차원 (Star Schema Dimension).
 *
 * dateKey = year * 10 + semester (예: 2024-1학기 → 20241).
 * 성적·제출 사실 테이블이 학년도/학기 단위로 집계되므로, 학사 기간을 하나의 차원으로 모델링한다.
 */
@Entity
@Table(name = "dim_date")
@Getter @Setter @NoArgsConstructor
public class DimDate {

    /** year*10 + semester */
    @Id
    @Column(name = "date_key")
    private Integer dateKey;

    private int year;

    private int semester;

    /** "2024-1" 형태 라벨 */
    @Column(length = 16)
    private String label;

    public DimDate(int year, int semester) {
        this.dateKey = year * 10 + semester;
        this.year = year;
        this.semester = semester;
        this.label = year + "-" + semester;
    }

    public static Integer keyOf(int year, int semester) {
        return year * 10 + semester;
    }
}
