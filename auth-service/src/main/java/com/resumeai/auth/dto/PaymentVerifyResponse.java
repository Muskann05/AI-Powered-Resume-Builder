package com.resumeai.auth.dto;

public record PaymentVerifyResponse(
        boolean verified,
        String message
) {
}
