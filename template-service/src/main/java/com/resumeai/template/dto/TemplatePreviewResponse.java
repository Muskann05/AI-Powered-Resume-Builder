package com.resumeai.template.dto;

public record TemplatePreviewResponse(
        String templateId,
        String name,
        String thumbnailUrl,
        String previewHtml
) {
}
