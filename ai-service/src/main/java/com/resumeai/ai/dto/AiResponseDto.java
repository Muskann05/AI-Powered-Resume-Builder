package com.resumeai.ai.dto;

import com.resumeai.ai.enums.AiModel;
import com.resumeai.ai.enums.RequestStatus;
import com.resumeai.ai.enums.RequestType;

import java.time.LocalDateTime;

public record AiResponseDto(
        String requestId,
        String userId,
        String resumeId,
        RequestType requestType,
        String aiResponse,
        AiModel model,
        Integer tokensUsed,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
