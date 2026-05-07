package com.resumeai.jobmatch.dto;

public record NotificationRequest(
        String recipientId,
        String type,
        String title,
        String message,
        String relatedId,
        String actionUrl
) {
}