package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record TailorResumeRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String resumeJson,
        @NotBlank String jobDescription
) {}
