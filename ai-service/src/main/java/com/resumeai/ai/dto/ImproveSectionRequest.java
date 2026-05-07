package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ImproveSectionRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String sectionName,
        @NotBlank String currentContent
) {}
