package com.resumeai.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentOrderRequest(
        @NotBlank String plan
) {
}
