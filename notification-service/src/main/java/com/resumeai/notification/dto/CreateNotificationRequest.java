package com.resumeai.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
        @NotBlank String recipientId,
        @NotBlank String type,
        @NotBlank String title,
        @NotBlank String message,
        @NotBlank String channel,
        String relatedId,
        String relatedType,
        String actionUrl
) {
}