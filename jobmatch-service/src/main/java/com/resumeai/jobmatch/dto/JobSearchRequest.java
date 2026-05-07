package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;

public record JobSearchRequest(
        @NotBlank String resumeId,
        @NotBlank String userId,
        @NotBlank String title,
        @NotBlank String location,
        Integer limit
) {
}