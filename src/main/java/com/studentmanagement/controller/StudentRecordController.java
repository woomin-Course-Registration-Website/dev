package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.record.StudentRecordRequest;
import com.studentmanagement.service.StudentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 학생부(학생 기록) 관리 컨트롤러
 *
 * - 학생 1명당 학생부 레코드는 1개입니다 (OneToOne).
 * - 최초 수정 요청 시 레코드가 없으면 자동 생성됩니다.
 * - 출결 정보는 JSON 형태로 저장됩니다: {"present": 180, "absent": 2, "late": 3}
 */
@Tag(name = "학생부 관리", description = "학생 출결 및 특기사항 조회·수정 API")
@RestController
@RequestMapping("/api/students/{studentId}/records")
public class StudentRecordController {

    private final StudentRecordService recordService;

    public StudentRecordController(StudentRecordService recordService) {
        this.recordService = recordService;
    }

    @Operation(
        summary = "학생부 조회",
        description = "학생의 출결 정보와 특기사항을 조회합니다.\n\n" +
                      "출결(`attendance`) 필드 예시:\n" +
                      "```json\n{\"present\": 180, \"absent\": 2, \"late\": 3}\n```"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 또는 학생부 없음")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getRecord(
            @Parameter(description = "학생 ID") @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(recordService.getRecord(studentId)));
    }

    @Operation(
        summary = "학생부 수정",
        description = "출결 정보와 특기사항을 수정합니다.\n\n" +
                      "- 학생부 레코드가 없으면 자동 생성됩니다.\n" +
                      "- `attendance`와 `specialNotes` 모두 선택값입니다. 미입력 시 해당 항목은 변경되지 않습니다.\n\n" +
                      "**요청 예시:**\n" +
                      "```json\n" +
                      "{\n" +
                      "  \"attendance\": {\"present\": 180, \"absent\": 2, \"late\": 3},\n" +
                      "  \"specialNotes\": \"수학 경시대회 금상 수상\"\n" +
                      "}\n" +
                      "```"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 없음")
    })
    @PutMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> update(
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @RequestBody StudentRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recordService.update(studentId, request)));
    }
}
