package com.resumeai.resume.dto;

import com.resumeai.resume.enums.ResumeStatus;

import java.time.LocalDateTime;

public record ResumeResponse(
        String resumeId,
        String userId,
        String title,
        String targetJobTitle,
        String templateId,
        Integer atsScore,
        ResumeStatus status,
        String language,
        Boolean isPublic,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
