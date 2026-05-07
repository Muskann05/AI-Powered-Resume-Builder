package com.resumeai.notification.dto;

import jakarta.validation.constraints.NotNull;

public record MarkReadRequest(@NotNull Boolean isRead) {
}