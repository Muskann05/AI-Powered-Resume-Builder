package com.resumeai.resume.dto;

public record ResumeAdminStatsResponse(
        long totalResumes,
        long publicResumes,
        long draftResumes,
        long completeResumes,
        long totalViews
) {}