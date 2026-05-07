package com.resumeai.export.entity;

import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.enums.ExportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "export_jobs")
public class ExportJob {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false, length = 36)
    private String jobId;

    @Column(name = "resume_id", nullable = false, length = 36)
    private String resumeId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportStatus status;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "template_id", nullable = false, length = 36)
    private String templateId;

    @Column(columnDefinition = "TEXT")
    private String customizations;

    @Column(name = "resume_data_json", columnDefinition = "LONGTEXT")
    private String resumeDataJson;

    @Column(name = "html_snapshot", columnDefinition = "LONGTEXT")
    private String htmlSnapshot;

    @Column(name = "css_snapshot", columnDefinition = "LONGTEXT")
    private String cssSnapshot;

    @Column(name = "download_token", length = 100)
    private String downloadToken;

    @Column(name = "download_token_expires_at")
    private LocalDateTime downloadTokenExpiresAt;

    @PrePersist
    public void prePersist() {
        if (jobId == null) jobId = UUID.randomUUID().toString();
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (status == null) status = ExportStatus.QUEUED;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getResumeId() { return resumeId; }
    public void setResumeId(String resumeId) { this.resumeId = resumeId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }

    public ExportStatus getStatus() { return status; }
    public void setStatus(ExportStatus status) { this.status = status; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Integer getFileSizeKb() { return fileSizeKb; }
    public void setFileSizeKb(Integer fileSizeKb) { this.fileSizeKb = fileSizeKb; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getCustomizations() { return customizations; }
    public void setCustomizations(String customizations) { this.customizations = customizations; }

    public String getResumeDataJson() { return resumeDataJson; }
    public void setResumeDataJson(String resumeDataJson) { this.resumeDataJson = resumeDataJson; }

    public String getHtmlSnapshot() { return htmlSnapshot; }
    public void setHtmlSnapshot(String htmlSnapshot) { this.htmlSnapshot = htmlSnapshot; }

    public String getCssSnapshot() { return cssSnapshot; }
    public void setCssSnapshot(String cssSnapshot) { this.cssSnapshot = cssSnapshot; }

    public String getDownloadToken() { return downloadToken; }
    public void setDownloadToken(String downloadToken) { this.downloadToken = downloadToken; }

    public LocalDateTime getDownloadTokenExpiresAt() { return downloadTokenExpiresAt; }
    public void setDownloadTokenExpiresAt(LocalDateTime downloadTokenExpiresAt) { this.downloadTokenExpiresAt = downloadTokenExpiresAt; }
}
