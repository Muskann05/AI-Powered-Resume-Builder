package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeJobFitRequest(
        @NotBlank String resumeId,
        @NotBlank String userId,
        @NotBlank String jobTitle,
        @NotBlank String jobDescription
) {
}