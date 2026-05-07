package com.resumeai.jobmatch.entity;

import com.resumeai.jobmatch.enums.JobSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_matches")
public class JobMatch {

    @Id
    @Column(name = "match_id", nullable = false, updatable = false, length = 36)
    private String matchId;

    @Column(name = "resume_id", nullable = false, length = 36)
    private String resumeId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "job_title", nullable = false, length = 200)
    private String jobTitle;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "job_location", length = 150)
    private String location;

    @Column(name = "external_job_id", length = 100)
    private String externalJobId;

    @Column(name = "apply_url", length = 500)
    private String applyUrl;

    @Column(name = "job_description", nullable = false, columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "recommendations", columnDefinition = "LONGTEXT")
    private String recommendations;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private JobSource source;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt;

    @PrePersist
    public void prePersist() {
        if (matchId == null) matchId = UUID.randomUUID().toString();
        if (matchedAt == null) matchedAt = LocalDateTime.now();
        if (isBookmarked == null) isBookmarked = false;
        if (source == null) source = JobSource.MANUAL;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getResumeId() { return resumeId; }
    public void setResumeId(String resumeId) { this.resumeId = resumeId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getExternalJobId() { return externalJobId; }
    public void setExternalJobId(String externalJobId) { this.externalJobId = externalJobId; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public JobSource getSource() { return source; }
    public void setSource(JobSource source) { this.source = source; }
    public Boolean getIsBookmarked() { return isBookmarked; }
    public void setIsBookmarked(Boolean bookmarked) { isBookmarked = bookmarked; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public void setMatchedAt(LocalDateTime matchedAt) { this.matchedAt = matchedAt; }
}