package com.resumeai.export.messaging;

import java.io.Serializable;

public class ExportMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;

    public ExportMessage() {
    }

    public ExportMessage(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
