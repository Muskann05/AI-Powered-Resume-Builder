package com.resumeai.auth.service;

import com.resumeai.auth.client.NotificationServiceClient;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.ForgotPasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RefreshTokenRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.ResetPasswordRequest;
import com.resumeai.auth.dto.UpdateProfileRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuthEventPublisher authEventPublisher;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private AuditLogService auditLogService;

    private AuthServiceImpl authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                tokenBlacklistService,
                authEventPublisher,
                passwordResetTokenRepository,
                notificationServiceClient,
                auditLogService
        );

        ReflectionTestUtils.setField(authService, "resetPasswordUrl", "http://localhost:4200/reset-password");

        user = new User();
        user.setUserId("user-101");
        user.setFullName("Muskan");
        user.setEmail("muskan@example.com");
        user.setPasswordHash("encoded-password");
        user.setPhone("9999999999");
        user.setRole(Role.USER);
        user.setProvider(AuthProvider.LOCAL);
        user.setSubscriptionPlan(SubscriptionPlan.FREE);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void registerShouldCreateNewUser() {
        RegisterRequest request = new RegisterRequest(
                "Muskan",
                "muskan@example.com",
                "password123",
                "9999999999"
        );

        when(userRepository.existsByEmail("muskan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId("user-101");
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        var response = authService.register(request);

        assertEquals("user-101", response.userId());
        assertEquals("muskan@example.com", response.email());
        assertEquals(Role.USER, response.role());
        assertEquals(SubscriptionPlan.FREE, response.subscriptionPlan());

        verify(userRepository).save(any(User.class));
        verify(auditLogService).record(
                "muskan@example.com",
                "REGISTER",
                "USER",
                "user-101",
                "User registered"
        );
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Muskan",
                "muskan@example.com",
                "password123",
                "9999999999"
        );

        when(userRepository.existsByEmail("muskan@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void loginShouldReturnTokensForActiveUser() {
        LoginRequest request = new LoginRequest("muskan@example.com", "password123");

        when(userRepository.findByEmail("muskan@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateRefreshToken(user.getEmail())).thenReturn("refresh-token");
        when(jwtService.generateAccessToken(eq(user.getEmail()), any(Map.class))).thenReturn("access-token");

        var response = authService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("user-101", response.user().userId());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(auditLogService).record(
                "muskan@example.com",
                "LOGIN",
                "USER",
                "user-101",
                "User logged in"
        );
    }

    @Test
    void loginShouldRejectInactiveUser() {
        LoginRequest request = new LoginRequest("muskan@example.com", "password123");
        user.setIsActive(false);

        when(userRepository.findByEmail("muskan@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void logoutShouldBlacklistToken() {
        when(jwtService.getRemainingValidityMillis("access-token")).thenReturn(60000L);

        authService.logout("Bearer access-token");

        verify(tokenBlacklistService).blacklist("access-token", 60000L);
        verify(auditLogService).record(
                "CURRENT_USER",
                "LOGOUT",
                "TOKEN",
                null,
                "JWT token blacklisted"
        );
    }

    @Test
    void logoutShouldRejectMissingBearerHeader() {
        assertThrows(BadRequestException.class, () -> authService.logout("access-token"));
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        when(tokenBlacklistService.isBlacklisted("good-token")).thenReturn(false);
        when(jwtService.extractUsername("good-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("good-token", user.getEmail())).thenReturn(true);

        assertTrue(authService.validateToken("good-token"));
    }

    @Test
    void validateTokenShouldReturnFalseForBlacklistedToken() {
        when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        assertFalse(authService.validateToken("blacklisted-token"));
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        when(tokenBlacklistService.isBlacklisted("bad-token")).thenReturn(false);
        when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("invalid"));

        assertFalse(authService.validateToken("bad-token"));
    }

    @Test
    void refreshTokenShouldReturnNewAccessTokenWithSameRefreshToken() {
        when(jwtService.extractUsername("refresh-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-token", user.getEmail())).thenReturn(true);
        when(jwtService.generateAccessToken(eq(user.getEmail()), any(Map.class))).thenReturn("new-access-token");

        var response = authService.refreshToken(new RefreshTokenRequest("refresh-token"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void refreshTokenShouldRejectInvalidRefreshToken() {
        when(jwtService.extractUsername("refresh-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-token", user.getEmail())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(new RefreshTokenRequest("refresh-token")));
    }

    @Test
    void getUserByIdShouldReturnUser() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        var response = authService.getUserById("user-101");

        assertEquals("user-101", response.userId());
        assertEquals("muskan@example.com", response.email());
    }

    @Test
    void getUserByEmailShouldThrowWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getUserByEmail("missing@example.com"));
    }

    @Test
    void getAllUsersShouldReturnUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        var response = authService.getAllUsers();

        assertEquals(1, response.size());
        assertEquals("user-101", response.get(0).userId());
    }

    @Test
    void getUsersBySubscriptionPlanShouldReturnMatchingUsers() {
        when(userRepository.findBySubscriptionPlan(SubscriptionPlan.FREE)).thenReturn(List.of(user));

        var response = authService.getUsersBySubscriptionPlan(SubscriptionPlan.FREE);

        assertEquals(1, response.size());
        assertEquals(SubscriptionPlan.FREE, response.get(0).subscriptionPlan());
    }

    @Test
    void updateProfileShouldPersistChanges() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.updateProfile(
                "user-101",
                new UpdateProfileRequest("Updated Name", "8888888888")
        );

        assertEquals("Updated Name", response.fullName());
        assertEquals("8888888888", response.phone());
    }

    @Test
    void changePasswordShouldUpdatePasswordForLocalUser() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password123")).thenReturn("new-encoded-password");

        authService.changePassword(
                "user-101",
                new ChangePasswordRequest("old-password", "new-password123")
        );

        assertEquals("new-encoded-password", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordShouldRejectOAuthUser() {
        user.setProvider(AuthProvider.GOOGLE);
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> authService.changePassword(
                "user-101",
                new ChangePasswordRequest("old-password", "new-password123")
        ));
    }

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.changePassword(
                "user-101",
                new ChangePasswordRequest("wrong-password", "new-password123")
        ));
    }

    @Test
    void updateSubscriptionShouldPublishEventAndAuditLog() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        authService.updateSubscription("user-101", SubscriptionPlan.PREMIUM);

        assertEquals(SubscriptionPlan.PREMIUM, user.getSubscriptionPlan());
        verify(userRepository).save(user);
        verify(authEventPublisher).publishPlanChanged(any(PlanChangedEvent.class));
        verify(auditLogService).record(
                eq("muskan@example.com"),
                eq("SUBSCRIPTION_CHANGE"),
                eq("USER"),
                eq("user-101"),
                anyString()
        );
    }

    @Test
    void deactivateAccountShouldMarkInactive() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        authService.deactivateAccount("user-101");

        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
    }

    @Test
    void suspendUserShouldMarkInactive() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        authService.suspendUser("user-101");

        assertFalse(user.getIsActive());
        verify(auditLogService).record("ADMIN", "USER_SUSPEND", "USER", "user-101", "User suspended");
    }

    @Test
    void reactivateUserShouldMarkActive() {
        user.setIsActive(false);
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        authService.reactivateUser("user-101");

        assertTrue(user.getIsActive());
        verify(auditLogService).record("ADMIN", "USER_REACTIVATE", "USER", "user-101", "User reactivated");
    }

    @Test
    void deleteUserShouldDeleteEntity() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        authService.deleteUser("user-101");

        verify(userRepository).delete(user);
        verify(auditLogService).record("ADMIN", "USER_DELETE", "USER", "user-101", "User deleted");
    }

    @Test
    void adminUpdateSubscriptionShouldPublishEventAndAuditLog() {
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));

        authService.adminUpdateSubscription("user-101", SubscriptionPlan.PREMIUM, "admin-101");

        assertEquals(SubscriptionPlan.PREMIUM, user.getSubscriptionPlan());
        verify(authEventPublisher).publishPlanChanged(any(PlanChangedEvent.class));
        verify(auditLogService).record(
                eq("admin-101"),
                eq("ADMIN_SUBSCRIPTION_CHANGE"),
                eq("USER"),
                eq("user-101"),
                anyString()
        );
    }

    @Test
    void forgotPasswordShouldCreateResetTokenAndSendNotificationWhenUserExists() {
        when(userRepository.findByEmail("muskan@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndUsedFalse("user-101")).thenReturn(List.of());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.forgotPassword(new ForgotPasswordRequest("muskan@example.com"));

        assertEquals("If the email is registered, a password reset link has been sent.", response.message());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(notificationServiceClient).sendNotification(any());
    }

    @Test
    void forgotPasswordShouldStillReturnSuccessMessageWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        var response = authService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        assertEquals("If the email is registered, a password reset link has been sent.", response.message());
    }

    @Test
    void validateResetTokenShouldReturnValidForActiveToken() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("reset-token");
        resetToken.setUsed(false);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));

        var response = authService.validateResetToken("reset-token");

        assertTrue(response.valid());
        assertEquals("Token is valid", response.message());
    }

    @Test
    void validateResetTokenShouldReturnInvalidForExpiredToken() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("reset-token");
        resetToken.setUsed(false);
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));

        var response = authService.validateResetToken("reset-token");

        assertFalse(response.valid());
        assertEquals("Reset token is invalid or expired", response.message());
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndMarkTokenUsed() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("reset-token");
        resetToken.setUserId("user-101");
        resetToken.setUsed(false);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));
        when(userRepository.findByUserId("user-101")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password123")).thenReturn("new-encoded-password");

        var response = authService.resetPassword(
                new ResetPasswordRequest("reset-token", "new-password123", "new-password123")
        );

        assertEquals("Password reset successful", response.message());
        assertEquals("new-encoded-password", user.getPasswordHash());
        assertTrue(resetToken.getUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPasswordShouldRejectMismatchedPasswords() {
        assertThrows(BadRequestException.class, () -> authService.resetPassword(
                new ResetPasswordRequest("reset-token", "new-password123", "different123")
        ));
    }
}
