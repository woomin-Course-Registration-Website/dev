package com.studentmanagement.dto.user;

import com.studentmanagement.domain.User;
import lombok.Getter;

@Getter
public class TeacherOptionResponse {
    private final Long id;
    private final String name;

    public TeacherOptionResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
    }
}
