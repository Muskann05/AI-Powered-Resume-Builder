package com.resumeai.export.messaging;

import java.io.Serializable;

public class ExportReadyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String userId;
    private String resumeId;
    private String fileUrl;

    public ExportReadyMessage() {
    }

    public ExportReadyMessage(String jobId, String userId, String resumeId, String fileUrl) {
        this.jobId = jobId;
        this.userId = userId;
        this.resumeId = resumeId;
        this.fileUrl = fileUrl;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
