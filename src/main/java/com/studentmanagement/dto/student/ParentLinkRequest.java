package com.studentmanagement.dto.student;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParentLinkRequest {

    @NotNull(message = "학부모 사용자 ID는 필수입니다.")
    private Long parentUserId;
}
