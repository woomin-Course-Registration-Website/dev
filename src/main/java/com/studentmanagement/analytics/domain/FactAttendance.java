package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 출결 사실 (Star Schema Fact) — 학생당 1행 스냅샷.
 *
 * 운영 student_records.attendance(JSON)를 파싱하여 present/absent/late로 적재한다.
 * PK가 studentId 자연키이므로 save()가 upsert가 된다.
 */
@Entity
@Table(name = "fact_attendance")
@Getter @Setter @NoArgsConstructor
public class FactAttendance {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    private int present;

    private int absent;

    private int late;

    public FactAttendance(Long studentId, int present, int absent, int late) {
        this.studentId = studentId;
        this.present = present;
        this.absent = absent;
        this.late = late;
    }

    /** 출석률 = present / (present+absent+late). 데이터 없으면 0. */
    @Transient
    public double attendanceRate() {
        int total = present + absent + late;
        return total == 0 ? 0.0 : (double) present / total;
    }
}
