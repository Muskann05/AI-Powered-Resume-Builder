package com.resumeai.template.dto;

public record AuthUserResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        String role,
        String provider,
        Boolean isActive,
        String subscriptionPlan,
        Object createdAt
) {
}
