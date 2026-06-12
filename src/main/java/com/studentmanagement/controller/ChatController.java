package com.studentmanagement.controller;

import com.studentmanagement.analytics.service.ChatService;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.dto.analytics.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 학습 분석 AI 챗봇 컨트롤러 (EP-08 선택 기능) — TEACHER/ADMIN 전용.
 *
 * LLM_API_KEY 미설정 시 503을 반환한다. 프론트는 enabled 여부로 챗봇 UI 노출을 제어한다.
 */
@Tag(name = "학습 분석 챗봇", description = "분석 데이터 기반 AI 질의 API (TEACHER/ADMIN, 선택 기능)")
@RestController
@RequestMapping("/api/analytics/chat")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "챗봇 활성 여부", description = "서버에 LLM_API_KEY가 설정되어 챗봇을 사용할 수 있는지 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(ApiResponse.ok(chatService.isEnabled()));
    }

    @Operation(
        summary = "학습 현황 질의",
        description = "분석 데이터에 근거해 질문에 답합니다. studentId를 주면 해당 학생으로 컨텍스트를 한정합니다."
    )
    @PostMapping
    public ResponseEntity<?> ask(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.ask(request.getMessage(), request.getStudentId())));
    }
}
