package com.resumeai.auth.service;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.enums.SubscriptionPlan;

import java.util.List;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String authHeader);
    boolean validateToken(String token);
    AuthResponse refreshToken(RefreshTokenRequest request);

    UserResponse getUserById(String userId);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    List<UserResponse> getUsersBySubscriptionPlan(SubscriptionPlan subscriptionPlan);

    UserResponse updateProfile(String userId, UpdateProfileRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    void updateSubscription(String userId, SubscriptionPlan subscriptionPlan);
    void deactivateAccount(String userId);

    void suspendUser(String userId);
    void reactivateUser(String userId);
    void deleteUser(String userId);
    void adminUpdateSubscription(String userId, SubscriptionPlan subscriptionPlan, String changedBy);

    ApiMessageResponse forgotPassword(ForgotPasswordRequest request);
    TokenValidationResponse validateResetToken(String token);
    ApiMessageResponse resetPassword(ResetPasswordRequest request);
}
