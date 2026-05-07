package com.resumeai.auth.dto;

import com.resumeai.auth.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionRequest(
        @NotNull SubscriptionPlan subscriptionPlan
) {
}
