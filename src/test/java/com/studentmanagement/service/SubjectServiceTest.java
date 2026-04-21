package com.studentmanagement.service;

import com.studentmanagement.domain.Subject;
import com.studentmanagement.dto.subject.SubjectRequest;
import com.studentmanagement.dto.subject.SubjectResponse;
import com.studentmanagement.fixture.TestFixtures;
import com.studentmanagement.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock SubjectRepository subjectRepository;

    @InjectMocks SubjectService subjectService;

    // ── getAll ────────────────────────────────────────────────────────

    @Test
    void getAll_returnsMappedResponses() {
        Subject subject = TestFixtures.subject();
        given(subjectRepository.findAll()).willReturn(List.of(subject));

        List<SubjectResponse> result = subjectService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("수학");
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_whenNewName_savesAndReturns() {
        SubjectRequest req = subjectRequest("영어");
        given(subjectRepository.existsByName("영어")).willReturn(false);
        given(subjectRepository.save(any(Subject.class))).willAnswer(inv -> {
            Subject s = inv.getArgument(0);
            TestFixtures.setId(s, 101L);
            return s;
        });

        SubjectResponse result = subjectService.create(req);

        assertThat(result.getName()).isEqualTo("영어");
    }

    @Test
    void create_whenDuplicateName_throwsIllegalArgumentException() {
        SubjectRequest req = subjectRequest("수학");
        given(subjectRepository.existsByName("수학")).willReturn(true);

        assertThatThrownBy(() -> subjectService.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private SubjectRequest subjectRequest(String name) {
        SubjectRequest r = new SubjectRequest();
        setField(r, "name", name);
        return r;
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
