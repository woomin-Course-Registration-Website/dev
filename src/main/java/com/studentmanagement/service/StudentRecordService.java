package com.studentmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.StudentRecord;
import com.studentmanagement.dto.record.StudentRecordRequest;
import com.studentmanagement.dto.record.StudentRecordResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.repository.StudentRecordRepository;
import com.studentmanagement.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;  // Spring Boot 자동 구성 빈 사용

    public StudentRecordService(StudentRecordRepository recordRepository,
                                StudentRepository studentRepository,
                                ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 학생부 조회
     *
     * @throws ResourceNotFoundException 학생부 레코드가 아직 생성되지 않은 경우
     */
    public StudentRecordResponse getRecord(Long studentId) {
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
        record.setSpecialNotes(request.getSpecialNotes());

        return new StudentRecordResponse(recordRepository.save(record));
    }
}
