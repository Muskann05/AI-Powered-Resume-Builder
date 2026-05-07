package com.resumeai.ai.dto;

import com.resumeai.ai.enums.AiModel;

public record AiUsageByModelResponse(
        AiModel model,
        long requests,
        long tokensUsed
) {}