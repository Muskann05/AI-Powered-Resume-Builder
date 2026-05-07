package com.resumeai.ai.dto;

public record NotificationRequest(
        String recipientId,
        String type,
        String title,
        String message,
        String channel,
        String relatedId,
        String relatedType,
        String actionUrl
) {
}
