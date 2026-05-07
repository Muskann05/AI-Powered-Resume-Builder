package com.resumeai.jobmatch.dto;

import com.resumeai.jobmatch.enums.JobSource;

import java.time.LocalDateTime;

public record JobMatchResponse(
        String matchId,
        String resumeId,
        String userId,
        String jobTitle,
        String companyName,
        String location,
        String externalJobId,
        String applyUrl,
        String jobDescription,
        Integer matchScore,
        String missingSkills,
        String recommendations,
        JobSource source,
        Boolean isBookmarked,
        LocalDateTime matchedAt
) {
}