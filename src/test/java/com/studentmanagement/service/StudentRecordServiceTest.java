package com.studentmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.StudentRecord;
import com.studentmanagement.dto.record.StudentRecordRequest;
import com.studentmanagement.dto.record.StudentRecordResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.StudentRecordRepository;
import com.studentmanagement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRecordServiceTest {

    @Mock StudentRecordRepository recordRepository;
    @Mock StudentRepository studentRepository;
    @Spy  ObjectMapper objectMapper;

    @InjectMocks StudentRecordService studentRecordService;

    private Student student;
    private StudentRecord record;

    @BeforeEach
    void setUp() {
        student = TestFixtures.student(TestFixtures.studentUser());
        record  = new StudentRecord(student);
        record.setSpecialNotes("기존 특기사항");
        TestFixtures.setId(record, 600L);
    }

    // ── getRecord ─────────────────────────────────────────────────────

    @Test
    void getRecord_whenExists_returnsResponse() {
        given(recordRepository.findByStudentId(10L)).willReturn(Optional.of(record));

        StudentRecordResponse result = studentRecordService.getRecord(10L);

        assertThat(result).isNotNull();
    }

    @Test
    void getRecord_whenNotFound_throwsResourceNotFoundException() {
        given(recordRepository.findByStudentId(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentRecordService.getRecord(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update (upsert) ───────────────────────────────────────────────

    @Test
    void update_whenRecordExists_updatesFields() {
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(recordRepository.findByStudentId(10L)).willReturn(Optional.of(record));
        given(recordRepository.save(any())).willReturn(record);
        StudentRecordRequest req = recordRequest("수정된 특기사항", null);

        StudentRecordResponse result = studentRecordService.update(10L, req);

        assertThat(result).isNotNull();
        assertThat(record.getSpecialNotes()).isEqualTo("수정된 특기사항");
    }

    @Test
    void update_whenRecordNotExists_createsNew() {
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(recordRepository.findByStudentId(10L)).willReturn(Optional.empty());
        given(recordRepository.save(any())).willAnswer(inv -> {
            StudentRecord r = inv.getArgument(0);
            TestFixtures.setId(r, 601L);
            return r;
        });
        StudentRecordRequest req = recordRequest("새 특기사항", null);

        StudentRecordResponse result = studentRecordService.update(10L, req);

        assertThat(result).isNotNull();
        verify(recordRepository).save(any(StudentRecord.class));
    }

    @Test
    void update_whenAttendanceNotNull_serializedToJson() throws Exception {
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(recordRepository.findByStudentId(10L)).willReturn(Optional.of(record));
        given(recordRepository.save(any())).willReturn(record);

        StudentRecordRequest req = recordRequestWithAttendance("특기사항", 180, 3, 5);

        studentRecordService.update(10L, req);

        assertThat(record.getAttendance()).contains("present");
        assertThat(record.getAttendance()).contains("absent");
    }

    @Test
    void update_whenAttendanceNull_doesNotChangeAttendance() {
        record.setAttendance("{\"present\":180}");
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(recordRepository.findByStudentId(10L)).willReturn(Optional.of(record));
        given(recordRepository.save(any())).willReturn(record);
        StudentRecordRequest req = recordRequest("특기사항", null);

        studentRecordService.update(10L, req);

        assertThat(record.getAttendance()).isEqualTo("{\"present\":180}");
    }

    @Test
    void update_whenStudentNotFound_throwsResourceNotFoundException() {
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentRecordService.update(999L, recordRequest("내용", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private StudentRecordRequest recordRequest(String specialNotes,
                                               StudentRecordRequest.AttendanceDto attendance) {
        StudentRecordRequest r = new StudentRecordRequest();
        setField(r, "specialNotes", specialNotes);
        setField(r, "attendance", attendance);
        return r;
    }

    private StudentRecordRequest recordRequestWithAttendance(String specialNotes,
                                                              int present, int absent, int late) {
        StudentRecordRequest.AttendanceDto att = new StudentRecordRequest.AttendanceDto();
        setField(att, "present", present);
        setField(att, "absent", absent);
        setField(att, "late", late);
        return recordRequest(specialNotes, att);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
