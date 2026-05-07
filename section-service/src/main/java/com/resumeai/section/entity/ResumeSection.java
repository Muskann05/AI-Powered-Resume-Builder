package com.resumeai.section.entity;

import com.resumeai.section.enums.SectionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_sections")
public class ResumeSection {

    @Id
    @Column(name = "section_id", nullable = false, updatable = false, length = 36)
    private String sectionId;

    @Column(name = "resume_id", nullable = false, length = 36)
    private String resumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 30)
    private SectionType sectionType;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Column(name = "ai_generated", nullable = false)
    private Boolean aiGenerated;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (sectionId == null) sectionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (displayOrder == null) displayOrder = 1;
        if (isVisible == null) isVisible = true;
        if (aiGenerated == null) aiGenerated = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getSectionId() { 
    	return sectionId; 
    }
    public void setSectionId(String sectionId) { 
    	this.sectionId = sectionId; 
    }
    public String getResumeId() {
    	return resumeId; 
    }
    public void setResumeId(String resumeId) { 
    	this.resumeId = resumeId; 
    }
    public SectionType getSectionType() { 
    	return sectionType; 
    }
    public void setSectionType(SectionType sectionType) { 
    	this.sectionType = sectionType; 
    }
    public String getTitle() { 
    	return title; 
    }
    public void setTitle(String title) { 
    	this.title = title; 
    }
    public String getContent() { 
    	return content; 
    }
    public void setContent(String content) { 
    	this.content = content; 
    }
    public Integer getDisplayOrder() { 
    	return displayOrder; 
    }
    public void setDisplayOrder(Integer displayOrder) { 
    	this.displayOrder = displayOrder; 
    }
    public Boolean getIsVisible() { 
    	return isVisible; 
    }
    public void setIsVisible(Boolean visible) { 
    	isVisible = visible; 
    }
    public Boolean getAiGenerated() { 
    	return aiGenerated; 
    }
    public void setAiGenerated(Boolean aiGenerated) { 
    	this.aiGenerated = aiGenerated; 
    }
    public LocalDateTime getCreatedAt() { 
    	return createdAt; 
    }
    public LocalDateTime getUpdatedAt() { 
    	return updatedAt; 
    }
}
