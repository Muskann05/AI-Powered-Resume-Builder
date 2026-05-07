package com.resumeai.auth.dto;

import com.resumeai.auth.enums.AuthProvider;
import com.resumeai.auth.enums.Role;
import com.resumeai.auth.enums.SubscriptionPlan;
import java.time.LocalDateTime;

public record UserResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        Role role,
        AuthProvider provider,
        Boolean isActive,
        SubscriptionPlan subscriptionPlan,
        LocalDateTime createdAt) {}
