package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record JobFitRequest(
        @NotBlank String userId,
        @NotBlank String resumeId,
        @NotBlank String resumeContent,
        @NotBlank String jobTitle,
        @NotBlank String jobDescription
) {
}
