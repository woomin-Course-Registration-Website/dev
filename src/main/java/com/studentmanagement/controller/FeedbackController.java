package com.studentmanagement.controller;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.feedback.FeedbackRequest;
import com.studentmanagement.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 피드백 관리 컨트롤러
 *
 * - 교사가 학생에게 작성하는 피드백을 관리합니다.
 * - 피드백에는 공개 여부(isPublic) 설정이 있습니다.
 *   - isPublic=true  : TEACHER / STUDENT(본인) / PARENT(자녀) 모두 조회 가능
 *   - isPublic=false : TEACHER만 조회 가능
 * - 수정/삭제는 작성한 교사 본인만 가능합니다.
 *
 * 카테고리(category) 종류:
 *   GRADE(성적) / BEHAVIOR(행동) / ATTENDANCE(출결) / ATTITUDE(태도) / OTHER(기타)
 */
@Tag(name = "피드백 관리", description = "교사 → 학생 피드백 작성·조회·수정·삭제 API")
@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(
        summary = "피드백 목록 조회",
        description = "특정 학생의 피드백 목록을 반환합니다.\n\n" +
                      "- **TEACHER**: 공개/비공개 피드백 모두 조회\n" +
                      "- **STUDENT / PARENT**: `isPublic = true` 인 피드백만 조회"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/api/students/{studentId}/feedbacks")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getFeedbacks(
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            Authentication auth) {
        User.Role role = User.Role.valueOf(
                auth.getAuthorities().stream().findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElseThrow());
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.getFeedbacks(studentId, auth.getName(), role)));
    }

    @Operation(
        summary = "피드백 작성",
        description = "학생에게 피드백을 작성합니다.\n\n" +
                      "**category 값:** `GRADE` / `BEHAVIOR` / `ATTENDANCE` / `ATTITUDE` / `OTHER`\n\n" +
                      "**요청 예시:**\n" +
                      "```json\n" +
                      "{\n" +
                      "  \"category\": \"GRADE\",\n" +
                      "  \"content\": \"수학 성적이 많이 향상되었습니다. 꾸준히 노력해주세요.\",\n" +
                      "  \"isPublic\": true\n" +
                      "}\n" +
                      "```"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "학생 없음")
    })
    @PostMapping("/api/students/{studentId}/feedbacks")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> create(
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @Valid @RequestBody FeedbackRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(feedbackService.create(studentId, request, auth.getName())));
    }

    @Operation(
        summary = "피드백 수정",
        description = "피드백 내용·카테고리·공개여부를 수정합니다.\n\n**작성한 교사 본인만 수정 가능합니다.**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드백 없음")
    })
    @PutMapping("/api/feedbacks/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> update(
            @Parameter(description = "피드백 ID") @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.update(id, request, auth.getName())));
    }

    @Operation(
        summary = "피드백 삭제",
        description = "피드백을 삭제합니다.\n\n**작성한 교사 본인만 삭제 가능합니다.**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드백 없음")
    })
    @DeleteMapping("/api/feedbacks/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> delete(
            @Parameter(description = "피드백 ID") @PathVariable Long id,
            Authentication auth) {
        feedbackService.delete(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "피드백이 삭제되었습니다."));
    }
}
