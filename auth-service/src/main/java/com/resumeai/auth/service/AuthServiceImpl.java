package com.resumeai.auth.service;

import com.resumeai.auth.client.NotificationServiceClient;
import com.resumeai.auth.dto.ApiMessageResponse;
import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.ForgotPasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.NotificationRequest;
import com.resumeai.auth.dto.RefreshTokenRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.ResetPasswordRequest;
import com.resumeai.auth.dto.TokenValidationResponse;
import com.resumeai.auth.dto.UpdateProfileRequest;
import com.resumeai.auth.dto.UserResponse;
import com.resumeai.auth.entity.PasswordResetToken;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.enums.AuthProvider;
import com.resumeai.auth.enums.Role;
import com.resumeai.auth.enums.SubscriptionPlan;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.exception.ResourceNotFoundException;
import com.resumeai.auth.exception.UnauthorizedException;
import com.resumeai.auth.messaging.PlanChangedEvent;
import com.resumeai.auth.repository.PasswordResetTokenRepository;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthEventPublisher authEventPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final AuditLogService auditLogService;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           TokenBlacklistService tokenBlacklistService,
                           AuthEventPublisher authEventPublisher,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           NotificationServiceClient notificationServiceClient,
                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.authEventPublisher = authEventPublisher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.notificationServiceClient = notificationServiceClient;
        this.auditLogService = auditLogService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.USER);
        user.setProvider(AuthProvider.LOCAL);
        user.setSubscriptionPlan(SubscriptionPlan.FREE);
        user.setIsActive(true);

        User saved = userRepository.save(user);
        auditLogService.record(saved.getEmail(), "REGISTER", "USER", saved.getUserId(), "User registered");

        return map(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Account is deactivated");
        }

        auditLogService.record(user.getEmail(), "LOGIN", "USER", user.getUserId(), "User logged in");
        return buildAuthResponse(user);
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Authorization header is required");
        }

        String token = authHeader.substring(7);
        long ttl = jwtService.getRemainingValidityMillis(token);
        tokenBlacklistService.blacklist(token, ttl);

        auditLogService.record("CURRENT_USER", "LOGOUT", "TOKEN", null, "JWT token blacklisted");
    }

    @Override
    public boolean validateToken(String token) {
        try {
            if (tokenBlacklistService.isBlacklisted(token)) {
                return false;
            }

            String email = jwtService.extractUsername(token);
            return userRepository.findByEmail(email)
                    .map(u -> jwtService.isTokenValid(token, u.getEmail()))
                    .orElse(false);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String email = jwtService.extractUsername(request.refreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!jwtService.isTokenValid(request.refreshToken(), user.getEmail())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        return buildAuthResponse(user, request.refreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        return map(getUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return map(userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersBySubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        return userRepository.findBySubscriptionPlan(subscriptionPlan).stream().map(this::map).toList();
    }

    @Override
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setFullName(request.fullName());
        user.setPhone(request.phone());

        User saved = userRepository.save(user);
        auditLogService.record(saved.getEmail(), "PROFILE_UPDATE", "USER", saved.getUserId(), "User profile updated");

        return map(saved);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = getUser(userId);

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password change is allowed only for local users");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        auditLogService.record(user.getEmail(), "PASSWORD_CHANGE", "USER", user.getUserId(), "Password changed");
    }

    @Override
    public void updateSubscription(String userId, SubscriptionPlan subscriptionPlan) {
        User user = getUser(userId);
        SubscriptionPlan oldPlan = user.getSubscriptionPlan();
        user.setSubscriptionPlan(subscriptionPlan);
        userRepository.save(user);

        publishPlanChange(user.getUserId(), oldPlan, subscriptionPlan, user.getUserId());
        auditLogService.record(user.getEmail(), "SUBSCRIPTION_CHANGE", "USER", userId,
                "Plan changed from " + oldPlan + " to " + subscriptionPlan);
    }

    @Override
    public void deactivateAccount(String userId) {
        User user = getUser(userId);
        user.setIsActive(false);
        userRepository.save(user);

        auditLogService.record(user.getEmail(), "ACCOUNT_DEACTIVATE", "USER", userId, "User deactivated own account");
    }

    @Override
    public void suspendUser(String userId) {
        User user = getUser(userId);
        user.setIsActive(false);
        userRepository.save(user);

        auditLogService.record("ADMIN", "USER_SUSPEND", "USER", userId, "User suspended");
    }

    @Override
    public void reactivateUser(String userId) {
        User user = getUser(userId);
        user.setIsActive(true);
        userRepository.save(user);

        auditLogService.record("ADMIN", "USER_REACTIVATE", "USER", userId, "User reactivated");
    }

    @Override
    public void deleteUser(String userId) {
        User user = getUser(userId);
        userRepository.delete(user);

        auditLogService.record("ADMIN", "USER_DELETE", "USER", userId, "User deleted");
    }

    @Override
    public void adminUpdateSubscription(String userId, SubscriptionPlan subscriptionPlan, String changedBy) {
        User user = getUser(userId);
        SubscriptionPlan oldPlan = user.getSubscriptionPlan();
        user.setSubscriptionPlan(subscriptionPlan);
        userRepository.save(user);

        publishPlanChange(userId, oldPlan, subscriptionPlan, changedBy);
        auditLogService.record(changedBy, "ADMIN_SUBSCRIPTION_CHANGE", "USER", userId,
                "Plan changed from " + oldPlan + " to " + subscriptionPlan);
    }

    @Override
    public ApiMessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            invalidateOldTokens(user.getUserId());

            String rawToken = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getUserId());
            resetToken.setEmail(user.getEmail());
            resetToken.setToken(rawToken);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            resetToken.setUsed(false);

            passwordResetTokenRepository.save(resetToken);

            String link = resetPasswordUrl + "?token=" + rawToken;

            notificationServiceClient.sendNotification(new NotificationRequest(
                    user.getUserId(),
                    "PASSWORD_RESET",
                    "Reset your ResumeAI password",
                    "Click the link to reset your password: " + link,
                    "EMAIL",
                    user.getUserId(),
                    "USER",
                    link
            ));

            auditLogService.record(user.getEmail(), "PASSWORD_RESET_REQUEST", "USER", user.getUserId(),
                    "Password reset link requested");
        });

        return new ApiMessageResponse("If the email is registered, a password reset link has been sent.");
    }

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponse validateResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token).orElse(null);

        if (resetToken == null || Boolean.TRUE.equals(resetToken.getUsed())
                || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new TokenValidationResponse(false, "Reset token is invalid or expired");
        }

        return new TokenValidationResponse(true, "Token is valid");
    }

    @Override
    public ApiMessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadRequestException("Reset token is invalid or expired"));

        if (Boolean.TRUE.equals(resetToken.getUsed())
                || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token is invalid or expired");
        }

        User user = getUser(resetToken.getUserId());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        auditLogService.record(user.getEmail(), "PASSWORD_RESET", "USER", user.getUserId(), "Password reset completed");

        return new ApiMessageResponse("Password reset successful");
    }

    private void publishPlanChange(String userId, SubscriptionPlan oldPlan, SubscriptionPlan newPlan, String changedBy) {
        authEventPublisher.publishPlanChanged(
                new PlanChangedEvent(
                        userId,
                        oldPlan != null ? oldPlan.name() : null,
                        newPlan != null ? newPlan.name() : null,
                        changedBy,
                        LocalDateTime.now()
                )
        );
    }

    private void invalidateOldTokens(String userId) {
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(userId);
        for (PasswordResetToken token : activeTokens) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
        }
    }

    private User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return buildAuthResponse(user, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "subscriptionPlan", user.getSubscriptionPlan().name(),
                "userId", user.getUserId()
        ));

        return new AuthResponse(accessToken, refreshToken, map(user));
    }

    private UserResponse map(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getProvider(),
                user.getIsActive(),
                user.getSubscriptionPlan(),
                user.getCreatedAt()
        );
    }
}
