package com.studentmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.StudentRecord;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.record.StudentRecordRequest;
import com.studentmanagement.dto.record.StudentRecordResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.domain.StudentRecordNote;
import com.studentmanagement.dto.record.RecordNoteRequest;
import com.studentmanagement.dto.record.RecordNoteResponse;
import com.studentmanagement.repository.StudentRecordNoteRepository;
import com.studentmanagement.repository.StudentRecordRepository;
import com.studentmanagement.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학생부 서비스
 *
 * 학생의 출결 정보와 특기사항을 관리합니다.
 * 학생 1명당 레코드 1개(OneToOne)이며, 최초 수정 요청 시 레코드가 없으면 자동 생성됩니다.
 *
 * 출결 정보(attendance)는 Jackson ObjectMapper를 통해 JSON 문자열로 직렬화하여 저장합니다.
 */
@Service
@Transactional(readOnly = true)
public class StudentRecordService {

    private final StudentRecordRepository recordRepository;
    private final StudentRecordNoteRepository noteRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;  // Spring Boot 자동 구성 빈 사용
    private final StudentAccessService studentAccessService;

    public StudentRecordService(StudentRecordRepository recordRepository,
                                StudentRecordNoteRepository noteRepository,
                                StudentRepository studentRepository,
                                ObjectMapper objectMapper,
                                StudentAccessService studentAccessService) {
        this.recordRepository = recordRepository;
        this.noteRepository = noteRepository;
        this.studentRepository = studentRepository;
        this.objectMapper = objectMapper;
        this.studentAccessService = studentAccessService;
    }

    /**
     * 학생부 조회
     * STUDENT는 본인, PARENT는 연동된 자녀만 조회 가능합니다.
     */
    public StudentRecordResponse getRecord(Long studentId, String requesterEmail, User.Role role) {
        studentAccessService.check(studentId, requesterEmail, role);
        StudentRecord record = recordRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생부를 찾을 수 없습니다."));
        return new StudentRecordResponse(record);
    }

    /**
     * 학생부 수정 (upsert)
     *
     * 레코드가 없으면 새로 생성하고, 있으면 기존 레코드를 수정합니다.
     * attendance 필드는 AttendanceDto를 JSON 문자열로 직렬화하여 저장합니다.
     *
     * @throws IllegalArgumentException attendance 직렬화 실패 시
     */
    @Transactional
    public StudentRecordResponse update(Long studentId, StudentRecordRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        // 레코드가 없으면 새로 생성 (upsert)
        StudentRecord record = recordRepository.findByStudentId(studentId)
                .orElseGet(() -> new StudentRecord(student));

        if (request.getAttendance() != null) {
            try {
                // AttendanceDto를 JSON 문자열로 변환하여 저장
                record.setAttendance(objectMapper.writeValueAsString(request.getAttendance()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("출결 데이터 형식이 올바르지 않습니다.");
            }
        }
        // specialNotes는 특기사항 다항목(StudentRecordNote)으로 이관됨 — 값이 있을 때만 갱신
        if (request.getSpecialNotes() != null) {
            record.setSpecialNotes(request.getSpecialNotes());
        }

        return new StudentRecordResponse(recordRepository.save(record));
    }

    // ── 특기사항 다항목 (StudentRecordNote) ──────────────────────────

    /** 특기사항 목록 조회. 레거시 단일 specialNotes가 있으면 1회 항목으로 이전한다. */
    @Transactional
    public List<RecordNoteResponse> listNotes(Long studentId, String requesterEmail, User.Role role) {
        studentAccessService.check(studentId, requesterEmail, role);
        StudentRecord record = recordRepository.findByStudentId(studentId).orElse(null);
        if (record == null) return List.of();
        migrateLegacyNote(record);
        return noteRepository.findByRecordIdOrderByCreatedAtAsc(record.getId())
                .stream().map(RecordNoteResponse::new).toList();
    }

    @Transactional
    public RecordNoteResponse addNote(Long studentId, RecordNoteRequest request) {
        StudentRecord record = getOrCreateRecord(studentId);
        migrateLegacyNote(record);
        StudentRecordNote note = noteRepository.save(new StudentRecordNote(record, request.getContent()));
        return new RecordNoteResponse(note);
    }

    @Transactional
    public RecordNoteResponse updateNote(Long noteId, RecordNoteRequest request) {
        StudentRecordNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("특기사항을 찾을 수 없습니다."));
        note.setContent(request.getContent());
        return new RecordNoteResponse(note);
    }

    @Transactional
    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new ResourceNotFoundException("특기사항을 찾을 수 없습니다.");
        }
        noteRepository.deleteById(noteId);
    }

    private StudentRecord getOrCreateRecord(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
        return recordRepository.findByStudentId(studentId)
                .orElseGet(() -> recordRepository.save(new StudentRecord(student)));
    }

    /** 레거시 단일 specialNotes를 첫 특기사항 항목으로 1회 이전 (이전 후 specialNotes는 비움). */
    private void migrateLegacyNote(StudentRecord record) {
        String legacy = record.getSpecialNotes();
        if (legacy != null && !legacy.isBlank()
                && noteRepository.findByRecordIdOrderByCreatedAtAsc(record.getId()).isEmpty()) {
            noteRepository.save(new StudentRecordNote(record, legacy));
            record.setSpecialNotes(null);
        }
    }
}
