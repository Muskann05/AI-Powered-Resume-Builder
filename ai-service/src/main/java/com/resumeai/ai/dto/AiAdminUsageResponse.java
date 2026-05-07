package com.resumeai.ai.dto;

import java.util.List;

public record AiAdminUsageResponse(
        long totalRequests,
        long completedRequests,
        long failedRequests,
        long totalTokensUsed,
        List<AiUsageByModelResponse> byModel,
        List<AiUsageByUserResponse> byUser
) {}