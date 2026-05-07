package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record CoverLetterRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String jobDescription,
        @NotBlank String applicantName
) {}
