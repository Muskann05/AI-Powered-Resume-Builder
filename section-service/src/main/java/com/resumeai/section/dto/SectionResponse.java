package com.resumeai.section.dto;

import com.resumeai.section.enums.SectionType;

import java.time.LocalDateTime;

public record SectionResponse(
        String sectionId,
        String resumeId,
        SectionType sectionType,
        String title,
        String content,
        Integer displayOrder,
        Boolean isVisible,
        Boolean aiGenerated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
