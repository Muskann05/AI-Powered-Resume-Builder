package com.resumeai.auth.dto;

public record CreatePaymentOrderResponse(
        String key,
        String orderId,
        Integer amount,
        String currency,
        String plan
) {
}
