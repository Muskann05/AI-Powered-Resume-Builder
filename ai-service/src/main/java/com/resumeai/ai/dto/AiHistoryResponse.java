package com.resumeai.ai.dto;

import com.resumeai.ai.enums.AiModel;
import com.resumeai.ai.enums.RequestStatus;
import com.resumeai.ai.enums.RequestType;

import java.time.LocalDateTime;

public record AiHistoryResponse(
        String requestId,
        String resumeId,
        RequestType requestType,
        AiModel model,
        Integer tokensUsed,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String aiResponse
) {}
