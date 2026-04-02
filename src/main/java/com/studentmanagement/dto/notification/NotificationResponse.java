package com.studentmanagement.dto.notification;

import com.studentmanagement.domain.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {
    private final Long id;
    private final String type;
    private final String message;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.type = n.getType().name();
        this.message = n.getMessage();
        this.isRead = n.isRead();
        this.createdAt = n.getCreatedAt();
    }
}
