package com.studentmanagement.controller;

import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 관리 컨트롤러
 *
 * - 본인에게 발송된 알림 목록 조회 및 읽음 처리를 담당합니다.
 * - 알림은 성적 입력·피드백 작성·상담 등록 시 서비스 레이어에서 자동 생성됩니다.
 * - 모든 엔드포인트는 로그인된 사용자 본인의 알림만 접근 가능합니다.
 *
 * 알림 타입(type): GRADE(성적) / FEEDBACK(피드백) / COUNSELING(상담)
 */
@Tag(name = "알림", description = "본인 알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
        summary = "알림 목록 조회",
        description = "로그인한 사용자의 알림 목록을 반환합니다.\n\n" +
                      "- 최신순(createdAt 내림차순)으로 정렬됩니다.\n" +
                      "- `isRead: false`인 항목이 읽지 않은 알림입니다.\n\n" +
                      "**알림 type 값:** `GRADE` / `FEEDBACK` / `COUNSELING`"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<?> getAll(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getMyNotifications(auth.getName())));
    }

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림 1개를 읽음 처리합니다. 본인의 알림만 처리 가능합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 알림 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림 없음")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "알림 ID") @PathVariable Long id,
            Authentication auth) {
        notificationService.markAsRead(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "읽음 처리되었습니다."));
    }

    @Operation(
        summary = "전체 알림 읽음 처리",
        description = "로그인한 사용자의 읽지 않은 알림을 모두 읽음 처리합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공")
    })
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "전체 읽음 처리되었습니다."));
    }
}
