package com.resumeai.auth.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PlanChangedEvent implements Serializable {

    private String userId;
    private String oldPlan;
    private String newPlan;
    private String changedBy;
    private LocalDateTime changedAt;

    public PlanChangedEvent() {
    }

    public PlanChangedEvent(String userId, String oldPlan, String newPlan, String changedBy, LocalDateTime changedAt) {
        this.userId = userId;
        this.oldPlan = oldPlan;
        this.newPlan = newPlan;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getOldPlan() {
        return oldPlan;
    }

    public String getNewPlan() {
        return newPlan;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setOldPlan(String oldPlan) {
        this.oldPlan = oldPlan;
    }

    public void setNewPlan(String newPlan) {
        this.newPlan = newPlan;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
