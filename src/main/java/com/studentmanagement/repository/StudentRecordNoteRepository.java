package com.studentmanagement.repository;

import com.studentmanagement.domain.StudentRecordNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRecordNoteRepository extends JpaRepository<StudentRecordNote, Long> {
    List<StudentRecordNote> findByRecordIdOrderByCreatedAtAsc(Long recordId);
}
