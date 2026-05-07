package com.resumeai.template.dto;

import com.resumeai.template.enums.TemplateCategory;

import java.time.LocalDateTime;

public record TemplateResponse(
        String templateId,
        String name,
        String description,
        String thumbnailUrl,
        String htmlLayout,
        String cssStyles,
        TemplateCategory category,
        Boolean isPremium,
        Boolean isActive,
        Integer usageCount,
        LocalDateTime createdAt
) {
}
