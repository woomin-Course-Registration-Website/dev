package com.studentmanagement.dto.assignment;

import com.studentmanagement.domain.Assignment;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class AssignmentResponse {
    private final Long id;
    private final Long subjectId;
    private final String subjectName;
    private final String title;
    private final LocalDate dueDate;
    private final int year;
    private final int semester;

    public AssignmentResponse(Assignment a) {
        this.id = a.getId();
        this.subjectId = a.getSubject().getId();
        this.subjectName = a.getSubject().getName();
        this.title = a.getTitle();
        this.dueDate = a.getDueDate();
        this.year = a.getYear();
        this.semester = a.getSemester();
    }
}
