package com.studentmanagement.dto.grade;

import com.studentmanagement.domain.Grade;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 반/과목별 성적 일괄 조회 응답 (성적 입력 화면용).
 * 여러 학생의 성적을 한 번에 반환하므로 studentId를 포함한다.
 */
@Getter
public class ClassGradeResponse {
    private final Long studentId;
    private final Long id;        // 성적 ID (수정/삭제용)
    private final BigDecimal score;
    private final String gradeRank;

    public ClassGradeResponse(Grade grade) {
        this.studentId = grade.getStudent().getId();
        this.id = grade.getId();
        this.score = grade.getScore();
        this.gradeRank = grade.getGradeRank();
    }
}
