package com.studentmanagement.dto.student;

import com.studentmanagement.domain.Student;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StudentResponse {
    private final Long id;
    private final String name;
    private final int grade;
    private final int classNum;
    private final int studentNum;
    private final Long userId;
    private final LocalDateTime createdAt;

    public StudentResponse(Student s) {
        this.id = s.getId();
        this.name = s.getName();
        this.grade = s.getGrade();
        this.classNum = s.getClassNum();
        this.studentNum = s.getStudentNum();
        this.userId = s.getUser() != null ? s.getUser().getId() : null;
        this.createdAt = s.getCreatedAt();
    }
}
