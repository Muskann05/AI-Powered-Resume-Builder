package com.resumeai.template.dto;

import com.resumeai.template.enums.TemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTemplateRequest(
        @NotBlank String name,
        String description,
        String thumbnailUrl,
        @NotBlank String htmlLayout,
        @NotBlank String cssStyles,
        @NotNull TemplateCategory category,
        @NotNull Boolean isPremium,
        @NotNull Boolean isActive
) {
}
