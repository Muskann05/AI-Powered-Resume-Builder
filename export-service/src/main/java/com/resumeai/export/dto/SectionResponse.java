package com.resumeai.export.dto;

public record SectionResponse(
        String sectionId,
        String resumeId,
        String sectionType,
        String title,
        String content,
        Integer displayOrder,
        Boolean isVisible,
        Boolean aiGenerated,
        Object createdAt,
        Object updatedAt
) {
}
