package com.resumeai.section.dto;

public record ResumeSummaryResponse(
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
        Object createdAt,
        Object updatedAt
) {
}
