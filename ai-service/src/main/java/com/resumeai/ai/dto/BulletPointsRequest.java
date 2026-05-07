package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record BulletPointsRequest(
        @NotBlank String userId,
        String resumeId,
        @NotBlank String jobRole,
        @NotBlank String companyName,
        @NotBlank String responsibilities
) {}
