package com.resumeai.auth.controller;

import com.resumeai.auth.dto.ApiMessageResponse;
import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.ForgotPasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RefreshTokenRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.ResetPasswordRequest;
import com.resumeai.auth.dto.TokenValidationResponse;
import com.resumeai.auth.dto.UpdateProfileRequest;
import com.resumeai.auth.dto.UpdateSubscriptionRequest;
import com.resumeai.auth.dto.UserResponse;
import com.resumeai.auth.enums.SubscriptionPlan;
import com.resumeai.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(new ApiMessageResponse("Logout successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(Map.of("valid", authService.validateToken(token)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<TokenValidationResponse> validateResetToken(@RequestParam String token) {
        return ResponseEntity.ok(authService.validateResetToken(token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getUserByEmail(authentication.getName()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @GetMapping("/users/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/by-plan")
    public ResponseEntity<List<UserResponse>> getUsersBySubscriptionPlan(@RequestParam SubscriptionPlan subscriptionPlan) {
        return ResponseEntity.ok(authService.getUsersBySubscriptionPlan(subscriptionPlan));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = authService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(authService.updateProfile(user.userId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiMessageResponse> changePassword(Authentication authentication,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        UserResponse user = authService.getUserByEmail(authentication.getName());
        authService.changePassword(user.userId(), request);
        return ResponseEntity.ok(new ApiMessageResponse("Password changed successfully"));
    }

    @PutMapping("/subscription")
    public ResponseEntity<ApiMessageResponse> updateSubscription(Authentication authentication,
                                                                 @Valid @RequestBody UpdateSubscriptionRequest request) {
        UserResponse user = authService.getUserByEmail(authentication.getName());
        authService.updateSubscription(user.userId(), request.subscriptionPlan());
        return ResponseEntity.ok(new ApiMessageResponse("Subscription updated successfully"));
    }

    @PutMapping("/deactivate")
    public ResponseEntity<ApiMessageResponse> deactivate(Authentication authentication) {
        UserResponse user = authService.getUserByEmail(authentication.getName());
        authService.deactivateAccount(user.userId());
        return ResponseEntity.ok(new ApiMessageResponse("Account deactivated successfully"));
    }

    @PutMapping("/admin/users/{userId}/subscription")
    public ResponseEntity<ApiMessageResponse> adminUpdateSubscription(Authentication authentication,
                                                                      @PathVariable String userId,
                                                                      @Valid @RequestBody UpdateSubscriptionRequest request) {
        String changedBy = authentication != null ? authentication.getName() : "ADMIN";
        authService.adminUpdateSubscription(userId, request.subscriptionPlan(), changedBy);
        return ResponseEntity.ok(new ApiMessageResponse("User subscription updated successfully"));
    }

    @PutMapping("/admin/users/{userId}/suspend")
    public ResponseEntity<ApiMessageResponse> suspendUser(@PathVariable String userId) {
        authService.suspendUser(userId);
        return ResponseEntity.ok(new ApiMessageResponse("User suspended successfully"));
    }

    @PutMapping("/admin/users/{userId}/reactivate")
    public ResponseEntity<ApiMessageResponse> reactivateUser(@PathVariable String userId) {
        authService.reactivateUser(userId);
        return ResponseEntity.ok(new ApiMessageResponse("User reactivated successfully"));
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<ApiMessageResponse> deleteUser(@PathVariable String userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok(new ApiMessageResponse("User deleted successfully"));
    }
}
