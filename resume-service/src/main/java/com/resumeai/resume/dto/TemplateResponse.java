package com.resumeai.resume.dto;

public record TemplateResponse(
        String templateId,
        String name,
        String description,
        String thumbnailUrl,
        String htmlLayout,
        String cssStyles,
        String category,
        Boolean isPremium,
        Boolean isActive,
        Integer usageCount,
        String createdAt
) {
}
