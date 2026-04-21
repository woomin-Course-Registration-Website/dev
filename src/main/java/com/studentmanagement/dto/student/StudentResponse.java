package com.studentmanagement.dto.student;

import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class StudentResponse {
    private final Long id;
    private final String name;
    private final int grade;
    private final int classNum;
    private final int studentNum;
    private final Long userId;
    private final LocalDateTime createdAt;
    private final List<ParentSummary> parents;

    public StudentResponse(Student s) {
        this.id = s.getId();
        this.name = s.getName();
        this.grade = s.getGrade();
        this.classNum = s.getClassNum();
        this.studentNum = s.getStudentNum();
        this.userId = s.getUser() != null ? s.getUser().getId() : null;
        this.createdAt = s.getCreatedAt();
        this.parents = s.getParents().stream().map(ParentSummary::new).toList();
    }

    @Getter
    public static class ParentSummary {
        private final Long id;
        private final String name;
        private final String email;

        public ParentSummary(User u) {
            this.id = u.getId();
            this.name = u.getName();
            this.email = u.getEmail();
        }
    }
}
