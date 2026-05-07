package com.resumeai.export.dto;

import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.enums.ExportStatus;

import java.time.LocalDateTime;

public record ExportJobResponse(
        String jobId,
        String resumeId,
        String userId,
        ExportFormat format,
        ExportStatus status,
        String fileUrl,
        Integer fileSizeKb,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt,
        String templateId,
        String customizations
) {
}
