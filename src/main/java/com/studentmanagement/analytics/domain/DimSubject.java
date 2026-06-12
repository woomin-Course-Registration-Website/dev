package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 과목 차원 (Star Schema Dimension).
 * PK는 운영 DB subject.id 자연키.
 */
@Entity
@Table(name = "dim_subject")
@Getter @Setter @NoArgsConstructor
public class DimSubject {

    @Id
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(nullable = false, length = 50)
    private String name;

    public DimSubject(Long subjectId, String name) {
        this.subjectId = subjectId;
        this.name = name;
    }
}
