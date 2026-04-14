package com.studentmanagement.dto.grade;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 과목별 성적 입력 현황 (대시보드용) */
@Getter
@AllArgsConstructor
public class GradeStatsItem {
    private String subjectName;
    private long   gradeCount;    // 성적 입력된 학생 수
    private long   studentCount;  // 전체 학생 수
}
