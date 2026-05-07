package com.resumeai.section.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkUpdateSectionsRequest(
        @NotEmpty List<@Valid SectionItem> sections
) {
    public record SectionItem(
            String sectionId,
            String resumeId,
            String sectionType,
            String title,
            String content,
            Integer displayOrder,
            Boolean isVisible,
            Boolean aiGenerated
    ) {}
}
