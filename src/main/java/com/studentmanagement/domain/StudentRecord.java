package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 학생부(학생 기록) 엔티티
 *
 * 학생 1명당 레코드 1개가 존재하는 OneToOne 관계입니다.
 * 최초 PUT 요청 시 레코드가 없으면 자동 생성됩니다.
 *
 * attendance 필드는 MySQL JSON 타입으로 저장됩니다.
 * 형식: {"present": 180, "absent": 2, "late": 3}
 * - present : 출석 일수
 * - absent  : 결석 일수
 * - late    : 지각 횟수
 */
@Entity
@Table(name = "student_records")
@Getter @Setter @NoArgsConstructor
public class StudentRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 학생부 대상 학생 (학생 삭제 시 학생부도 함께 삭제됨 — CASCADE) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    /**
     * 출결 정보 (JSON 문자열)
     * 예: {"present": 180, "absent": 2, "late": 3}
     */
    @Column(columnDefinition = "JSON")
    private String attendance;

    /** 특기사항 — 수상 실적, 활동 내역 등 자유 형식 텍스트 */
    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public StudentRecord(Student student) {
        this.student = student;
    }
}
