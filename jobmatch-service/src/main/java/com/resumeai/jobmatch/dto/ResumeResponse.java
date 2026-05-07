package com.resumeai.jobmatch.dto;

public record ResumeResponse(
        String resumeId,
        String userId,
        String title,
        String targetJobTitle,
        String templateId,
        Integer atsScore,
        String status,
        String language,
        Boolean isPublic,
        Integer viewCount,
        String createdAt,
        String updatedAt
) {
}