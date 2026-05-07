package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiJobFitRequest(
        @NotBlank String userId,
        @NotBlank String resumeId,
        @NotBlank String resumeTitle,
        String targetJobTitle,
        @NotBlank String jobTitle,
        @NotBlank String jobDescription,
        List<SectionResponse> sections
) {
}