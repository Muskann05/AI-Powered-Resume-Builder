package com.resumeai.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record BulkNotificationRequest(
        String subscriptionPlan,
        @NotBlank String type,
        @NotBlank String title,
        @NotBlank String message,
        @NotBlank String channel,
        String relatedId,
        String relatedType,
        String actionUrl
) {
}