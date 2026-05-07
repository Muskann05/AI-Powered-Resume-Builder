package com.resumeai.template.entity;

import com.resumeai.template.enums.TemplateCategory;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_templates")
public class ResumeTemplate {

    @Id
    @Column(name = "template_id", nullable = false, updatable = false, length = 36)
    private String templateId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "html_layout", nullable = false, columnDefinition = "LONGTEXT")
    private String htmlLayout;

    @Column(name = "css_styles", nullable = false, columnDefinition = "LONGTEXT")
    private String cssStyles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TemplateCategory category;

    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (templateId == null) templateId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isPremium == null) isPremium = false;
        if (isActive == null) isActive = true;
        if (usageCount == null) usageCount = 0;
    }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getHtmlLayout() { return htmlLayout; }
    public void setHtmlLayout(String htmlLayout) { this.htmlLayout = htmlLayout; }
    public String getCssStyles() { return cssStyles; }
    public void setCssStyles(String cssStyles) { this.cssStyles = cssStyles; }
    public TemplateCategory getCategory() { return category; }
    public void setCategory(TemplateCategory category) { this.category = category; }
    public Boolean getIsPremium() { return isPremium; }
    public void setIsPremium(Boolean premium) { isPremium = premium; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
