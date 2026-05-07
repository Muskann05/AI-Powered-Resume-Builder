package com.resumeai.ai.dto;

public record QuotaResponse(
        long remainingContentCalls,
        long remainingAtsChecks,
        boolean premium
) {}
