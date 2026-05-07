package com.resumeai.section.dto;

import com.resumeai.section.enums.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSectionRequest(
        @NotNull SectionType sectionType,
        @NotBlank String title,
        @NotBlank String content,
        Integer displayOrder,
        Boolean isVisible,
        Boolean aiGenerated
) {
}
