package com.resumeai.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        String notificationId,
        String recipientId,
        String type,
        String title,
        String message,
        String channel,
        String relatedId,
        String relatedType,
        String actionUrl,
        Boolean isRead,
        LocalDateTime sentAt
) {
}