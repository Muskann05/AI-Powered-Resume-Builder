package com.resumeai.jobmatch.dto;

public record SectionResponse(
        String sectionId,
        String resumeId,
        String sectionType,
        String title,
        String content,
        Integer displayOrder,
        Boolean isVisible,
        Boolean aiGenerated,
        String createdAt,
        String updatedAt
) {
}