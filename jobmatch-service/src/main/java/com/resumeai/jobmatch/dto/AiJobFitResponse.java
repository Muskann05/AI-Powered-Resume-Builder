package com.resumeai.jobmatch.dto;

public record AiJobFitResponse(
        Integer matchScore,
        String missingSkills,
        String recommendations
) {
}