package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record SummaryRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String jobTitle,
        @NotBlank String yearsOfExperience,
        @NotBlank String keySkills
) {}
