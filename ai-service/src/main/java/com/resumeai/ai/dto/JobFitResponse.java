package com.resumeai.ai.dto;

import java.util.List;

public record JobFitResponse(
        Integer matchScore,
        List<String> missingSkills,
        String recommendations
) {
}
