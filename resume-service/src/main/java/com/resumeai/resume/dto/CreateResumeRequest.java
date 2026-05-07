package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResumeRequest(
        @NotBlank String userId,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 150) String targetJobTitle,
        @NotBlank String templateId,
        String language
) {
}
