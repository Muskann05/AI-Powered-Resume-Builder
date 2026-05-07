package com.resumeai.resume.entity;

import com.resumeai.resume.enums.ResumeStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @Column(name = "resume_id", nullable = false, updatable = false, length = 36)
    private String resumeId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "target_job_title", length = 150)
    private String targetJobTitle;

    @Column(name = "template_id", nullable = false, length = 36)
    private String templateId;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeStatus status;

    @Column(nullable = false, length = 40)
    private String language;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (resumeId == null) resumeId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = ResumeStatus.DRAFT;
        if (language == null || language.isBlank()) language = "English";
        if (isPublic == null) isPublic = false;
        if (viewCount == null) viewCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getResumeId() { 
    	return resumeId; 
    }
    
    public void setResumeId(String resumeId) { 
    	this.resumeId = resumeId; 
    }
    
    public String getUserId() { 
    	return userId; 
    }
    
    public void setUserId(String userId) { 
    	this.userId = userId; 
    }
    
    public String getTitle() { 
    	return title; 
    }
    
    public void setTitle(String title) { 
    	this.title = title; 
    }
    
    public String getTargetJobTitle() { 
    	return targetJobTitle; 
    }
    
    public void setTargetJobTitle(String targetJobTitle) { 
    	this.targetJobTitle = targetJobTitle; 
    }
    
    public String getTemplateId() { 
    	return templateId; 
    }
    
    public void setTemplateId(String templateId) { 
    	this.templateId = templateId; 
    }
    
    public Integer getAtsScore() { 
    	return atsScore; 
    }
    
    public void setAtsScore(Integer atsScore) { 
    	this.atsScore = atsScore; 
    }
    
    public ResumeStatus getStatus() { 
    	return status; 
    }
    
    public void setStatus(ResumeStatus status) { 
    	this.status = status; 
    }
    
    public String getLanguage() { 
    	return language; 
    }
    
    public void setLanguage(String language) { 
    	this.language = language; 
    }
    public Boolean getIsPublic() { 
    	return isPublic; 
    }
    
    public void setIsPublic(Boolean aPublic) { 
    	isPublic = aPublic; 
    }
    
    public Integer getViewCount() { 
    	return viewCount; 
    }
    public void setViewCount(Integer viewCount) { 
    	this.viewCount = viewCount; 
    }
    
    public LocalDateTime getCreatedAt() { 
    	return createdAt; 
    }
    
    public LocalDateTime getUpdatedAt() { 
    	return updatedAt; 
    }
}
