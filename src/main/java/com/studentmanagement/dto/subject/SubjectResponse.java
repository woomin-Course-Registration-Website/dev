package com.studentmanagement.dto.subject;

import com.studentmanagement.domain.Subject;
import lombok.Getter;

@Getter
public class SubjectResponse {
    private final Long id;
    private final String name;

    public SubjectResponse(Subject subject) {
        this.id = subject.getId();
        this.name = subject.getName();
    }
}
