package com.resumeai.resume.dto;

import java.time.LocalDateTime;

public record SectionResponse(
        String sectionId,
        String resumeId,
        String sectionType,
        String title,
        String content,
        Integer displayOrder,
        Boolean isVisible,
        Boolean aiGenerated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
