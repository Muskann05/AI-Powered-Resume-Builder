package com.resumeai.export.dto;

import com.resumeai.export.enums.ExportFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExportRequest(
        @NotBlank String resumeId,
        @NotBlank String userId,
        @NotNull ExportFormat format,
        @NotBlank String templateId,
        String customizations,
        String resumeDataJson,
        String htmlSnapshot,
        String cssSnapshot
) {
}
