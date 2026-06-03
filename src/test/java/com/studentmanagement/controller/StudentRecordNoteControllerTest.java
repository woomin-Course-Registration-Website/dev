package com.studentmanagement.controller;

import com.studentmanagement.config.SecurityConfig;
import com.studentmanagement.domain.StudentRecordNote;
import com.studentmanagement.dto.record.RecordNoteResponse;
import com.studentmanagement.fixture.SecurityTestHelper;
import com.studentmanagement.service.StudentRecordService;
import com.studentmanagement.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.studentmanagement.fixture.SecurityTestHelper.FAKE_TOKEN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentRecordNoteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-minimum-32-characters!!",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
class StudentRecordNoteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  StudentRecordService studentRecordService;
    @MockBean  JwtUtil jwtUtil;

    @Test
    void listNotes_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentRecordService.listNotes(anyLong(), anyString(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/1/records/notes").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void listNotes_student_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        given(studentRecordService.listNotes(anyLong(), anyString(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/students/1/records/notes").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void addNote_teacher_returns201() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentRecordService.addNote(anyLong(), any()))
                .willReturn(new RecordNoteResponse(new StudentRecordNote()));

        mockMvc.perform(post("/api/students/1/records/notes")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"교내 수학경시 대상"}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void addNote_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(post("/api/students/1/records/notes")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"x"}
                    """))
                .andExpect(status().isForbidden());
    }

    @Test
    void addNote_blankContent_returns400() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);

        mockMvc.perform(post("/api/students/1/records/notes")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":""}
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNote_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentRecordService.updateNote(anyLong(), any()))
                .willReturn(new RecordNoteResponse(new StudentRecordNote()));

        mockMvc.perform(put("/api/record-notes/5")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"수정된 특기사항"}
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNote_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        willDoNothing().given(studentRecordService).deleteNote(anyLong());

        mockMvc.perform(delete("/api/record-notes/5").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNote_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(delete("/api/record-notes/5").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }
}
