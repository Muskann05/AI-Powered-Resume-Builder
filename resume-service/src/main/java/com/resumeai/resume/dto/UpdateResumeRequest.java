package com.resumeai.resume.dto;

import com.resumeai.resume.enums.ResumeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResumeRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 150) String targetJobTitle,
        @NotBlank String templateId,
        String language,
        ResumeStatus status
) {
}
