package com.resumeai.auth.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        String auditId,
        String actor,
        String action,
        String targetId,
        String targetType,
        String details,
        LocalDateTime createdAt
) {}