package com.studentmanagement.controller;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.record.RecordNoteRequest;
import com.studentmanagement.service.StudentRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 학생부 특기사항(다항목) 관리 컨트롤러
 *
 * - 목록/추가는 학생 컨텍스트(/students/{id}/records/notes)
 * - 수정/삭제는 항목 ID 기준(/record-notes/{noteId})
 */
@RestController
public class StudentRecordNoteController {

    private final StudentRecordService recordService;

    public StudentRecordNoteController(StudentRecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/api/students/{studentId}/records/notes")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','PARENT')")
    public ResponseEntity<?> listNotes(@PathVariable Long studentId, Authentication auth) {
        User.Role role = User.Role.valueOf(
                auth.getAuthorities().stream().findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElseThrow());
        return ResponseEntity.ok(ApiResponse.ok(recordService.listNotes(studentId, auth.getName(), role)));
    }

    @PostMapping("/api/students/{studentId}/records/notes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addNote(@PathVariable Long studentId, @Valid @RequestBody RecordNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(recordService.addNote(studentId, request)));
    }

    @PutMapping("/api/record-notes/{noteId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> updateNote(@PathVariable Long noteId, @Valid @RequestBody RecordNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recordService.updateNote(noteId, request)));
    }

    @DeleteMapping("/api/record-notes/{noteId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> deleteNote(@PathVariable Long noteId) {
        recordService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.ok(null, "특기사항이 삭제되었습니다."));
    }
}
