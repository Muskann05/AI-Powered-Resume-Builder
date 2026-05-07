package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record TranslateResumeRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String resumeText,
        @NotBlank String targetLanguage
) {}
