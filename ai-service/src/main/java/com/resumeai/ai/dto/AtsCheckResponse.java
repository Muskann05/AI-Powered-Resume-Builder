package com.resumeai.ai.dto;

import java.util.List;

public record AtsCheckResponse(
        String requestId,
        Integer atsScore,
        List<String> missingKeywords,
        String recommendation,
        String model
) {}
