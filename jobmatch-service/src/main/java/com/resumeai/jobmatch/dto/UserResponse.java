package com.resumeai.jobmatch.dto;

public record UserResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        String role,
        String provider,
        Boolean isActive,
        String subscriptionPlan,
        String createdAt
) {
}