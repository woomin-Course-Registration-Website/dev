package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 학생부 특기사항 항목 (학생부 1개당 여러 항목)
 *
 * 기존 StudentRecord.specialNotes(단일 텍스트)를 다항목으로 확장한 엔티티.
 */
@Entity
@Table(name = "student_record_notes")
@Getter @Setter @NoArgsConstructor
public class StudentRecordNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private StudentRecord record;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StudentRecordNote(StudentRecord record, String content) {
        this.record = record;
        this.content = content;
    }
}
