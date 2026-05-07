package com.resumeai.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReorderSectionsRequest(
        @NotBlank String sectionId,
        @NotNull Integer displayOrder
) {
}
