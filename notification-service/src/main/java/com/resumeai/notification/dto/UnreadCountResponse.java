package com.resumeai.notification.dto;

public record UnreadCountResponse(
        String recipientId,
        Long unreadCount
) {
}