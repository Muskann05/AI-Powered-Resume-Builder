package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AtsCheckRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String resumeText,
        @NotBlank String jobDescription
) {}
