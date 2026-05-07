package com.resumeai.ai.dto;

public record AiUsageByUserResponse(
        String userId,
        long requests,
        long tokensUsed
) {}