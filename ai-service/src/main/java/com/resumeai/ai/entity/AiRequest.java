package com.resumeai.ai.entity;

import com.resumeai.ai.enums.AiModel;
import com.resumeai.ai.enums.RequestStatus;
import com.resumeai.ai.enums.RequestType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_requests")
public class AiRequest {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false, length = 36)
    private String requestId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "resume_id", length = 36)
    private String resumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType requestType;

    @Column(name = "input_prompt", nullable = false, columnDefinition = "LONGTEXT")
    private String inputPrompt;

    @Column(name = "ai_response", columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AiModel model;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (requestId == null) requestId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = RequestStatus.QUEUED;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getResumeId() { return resumeId; }
    public void setResumeId(String resumeId) { this.resumeId = resumeId; }
    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }
    public String getInputPrompt() { return inputPrompt; }
    public void setInputPrompt(String inputPrompt) { this.inputPrompt = inputPrompt; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    public AiModel getModel() { return model; }
    public void setModel(AiModel model) { this.model = model; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
