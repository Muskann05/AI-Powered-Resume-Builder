package com.resumeai.jobmatch.dto;

public record LinkedInJobResponse(
        String externalJobId,
        String title,
        String companyName,
        String location,
        String description,
        String applyUrl
) {
}