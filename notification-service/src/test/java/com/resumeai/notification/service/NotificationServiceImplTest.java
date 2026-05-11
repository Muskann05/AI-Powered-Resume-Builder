package com.resumeai.notification.service;

import com.resumeai.notification.client.AuthServiceClient;
import com.resumeai.notification.dto.BulkNotificationRequest;
import com.resumeai.notification.dto.CreateNotificationRequest;
import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UserResponse;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.enums.NotificationChannel;
import com.resumeai.notification.enums.NotificationType;
import com.resumeai.notification.exception.BadRequestException;
import com.resumeai.notification.exception.ResourceNotFoundException;
import com.resumeai.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private JavaMailSender javaMailSender;

    private NotificationServiceImpl notificationService;

    private UserResponse activeFreeUser;
    private UserResponse activePremiumUser;
    private UserResponse inactiveUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                authServiceClient,
                javaMailSender
        );

        ReflectionTestUtils.setField(notificationService, "fromEmail", "noreply@resumeai.com");
        ReflectionTestUtils.setField(notificationService, "emailProvider", "smtp");
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);

        activeFreeUser = new UserResponse(
                "user-101",
                "Muskan Gupta",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "FREE",
                null
        );

        activePremiumUser = new UserResponse(
                "user-202",
                "Admin User",
                "admin@example.com",
                "8888888888",
                "ADMIN",
                "LOCAL",
                true,
                "PREMIUM",
                null
        );

        inactiveUser = new UserResponse(
                "user-303",
                "Inactive User",
                "inactive@example.com",
                "7777777777",
                "USER",
                "LOCAL",
                false,
                "FREE",
                null
        );
    }

    @Test
    void sendNotificationShouldPersistAppNotification() {
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        mockSave();

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "AI_DONE",
                "AI completed",
                "Your AI summary is ready",
                "APP",
                "resume-101",
                "RESUME",
                "/ai/history/user-101"
        );

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response.notificationId());
        assertEquals("user-101", response.recipientId());
        assertEquals("AI_DONE", response.type());
        assertEquals("AI completed", response.title());
        assertEquals("Your AI summary is ready", response.message());
        assertEquals("APP", response.channel());
        assertEquals("resume-101", response.relatedId());
        assertEquals("RESUME", response.relatedType());
        assertEquals("/ai/history/user-101", response.actionUrl());
        assertFalse(response.isRead());

        verify(notificationRepository).save(any(Notification.class));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotificationShouldSendEmailWhenChannelIsEmail() {
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        mockSave();

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "PASSWORD_RESET",
                "Reset password",
                "Click to reset password",
                "EMAIL",
                "user-101",
                "USER",
                "http://localhost:4200/reset-password?token=abc"
        );

        NotificationResponse response = notificationService.sendNotification(request);

        assertEquals("EMAIL", response.channel());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage mail = captor.getValue();
        assertEquals("noreply@resumeai.com", mail.getFrom());
        assertEquals("Reset password", mail.getSubject());
        assertEquals("Click to reset password\n\nAction: http://localhost:4200/reset-password?token=abc", mail.getText());
        assertEquals("muskan@example.com", mail.getTo()[0]);
    }

    @Test
    void sendNotificationShouldSkipEmailWhenEmailDisabled() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", false);

        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        mockSave();

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "PASSWORD_RESET",
                "Reset password",
                "Click to reset password",
                "EMAIL",
                null,
                null,
                null
        );

        NotificationResponse response = notificationService.sendNotification(request);

        assertEquals("EMAIL", response.channel());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotificationShouldSkipEmailWhenProviderIsLog() {
        ReflectionTestUtils.setField(notificationService, "emailProvider", "log");

        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        mockSave();

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "PASSWORD_RESET",
                "Reset password",
                "Click to reset password",
                "EMAIL",
                null,
                null,
                null
        );

        NotificationResponse response = notificationService.sendNotification(request);

        assertEquals("EMAIL", response.channel());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotificationShouldSkipEmailWhenUserEmailMissing() {
        UserResponse userWithoutEmail = new UserResponse(
                "user-101",
                "Muskan Gupta",
                "",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "FREE",
                null
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(userWithoutEmail);
        mockSave();

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "PASSWORD_RESET",
                "Reset password",
                "Click to reset password",
                "EMAIL",
                null,
                null,
                null
        );

        NotificationResponse response = notificationService.sendNotification(request);

        assertEquals("EMAIL", response.channel());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotificationShouldStillPersistWhenEmailSendingFails() {
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        mockSave();

        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "PASSWORD_RESET",
                "Reset password",
                "Click to reset password",
                "EMAIL",
                null,
                null,
                null
        );

        NotificationResponse response = notificationService.sendNotification(request);

        assertEquals("EMAIL", response.channel());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendNotificationShouldRejectMissingUser() {
        when(authServiceClient.getUserById("missing")).thenReturn(null);

        CreateNotificationRequest request = new CreateNotificationRequest(
                "missing",
                "AI_DONE",
                "AI completed",
                "Done",
                "APP",
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> notificationService.sendNotification(request));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotificationShouldRejectInvalidType() {
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "INVALID_TYPE",
                "Title",
                "Message",
                "APP",
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> notificationService.sendNotification(request));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotificationShouldRejectInvalidChannel() {
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);

        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-101",
                "AI_DONE",
                "Title",
                "Message",
                "SMS",
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> notificationService.sendNotification(request));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendBulkNotificationShouldSendToAllActiveUsersWhenPlanBlank() {
        when(authServiceClient.getAllUsers()).thenReturn(List.of(activeFreeUser, activePremiumUser, inactiveUser));
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        when(authServiceClient.getUserById("user-202")).thenReturn(activePremiumUser);
        mockSave();

        BulkNotificationRequest request = new BulkNotificationRequest(
                "",
                "PLAN_CHANGE",
                "New update",
                "ResumeAI has new features",
                "APP",
                null,
                null,
                "/dashboard"
        );

        List<NotificationResponse> responses = notificationService.sendBulkNotification(request);

        assertEquals(2, responses.size());
        verify(authServiceClient).getAllUsers();
        verify(authServiceClient, never()).getUserById("user-303");
    }

    @Test
    void sendBulkNotificationShouldSendToUsersByPlan() {
        when(authServiceClient.getUsersBySubscriptionPlan("PREMIUM")).thenReturn(List.of(activePremiumUser));
        when(authServiceClient.getUserById("user-202")).thenReturn(activePremiumUser);
        mockSave();

        BulkNotificationRequest request = new BulkNotificationRequest(
                "premium",
                "PLAN_CHANGE",
                "Premium update",
                "Premium feature added",
                "APP",
                null,
                null,
                "/subscription"
        );

        List<NotificationResponse> responses = notificationService.sendBulkNotification(request);

        assertEquals(1, responses.size());
        assertEquals("user-202", responses.get(0).recipientId());
        verify(authServiceClient).getUsersBySubscriptionPlan("PREMIUM");
    }

    @Test
    void getNotificationsByRecipientShouldReturnNotifications() {
        Notification notification = notification("notification-101", "user-101", NotificationType.AI_DONE, NotificationChannel.APP, false);

        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc("user-101"))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getNotificationsByRecipient("user-101");

        assertEquals(1, responses.size());
        assertEquals("notification-101", responses.get(0).notificationId());
    }

    @Test
    void getUnreadNotificationsShouldReturnUnreadNotifications() {
        Notification notification = notification("notification-101", "user-101", NotificationType.AI_DONE, NotificationChannel.APP, false);

        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        when(notificationRepository.findByRecipientIdAndIsReadOrderBySentAtDesc("user-101", false))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getUnreadNotifications("user-101");

        assertEquals(1, responses.size());
        assertFalse(responses.get(0).isRead());
    }

    @Test
    void markAsReadShouldUpdateReadState() {
        Notification notification = notification("notification-101", "user-101", NotificationType.AI_DONE, NotificationChannel.APP, false);

        when(notificationRepository.findByNotificationId("notification-101")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead("notification-101", true);

        assertTrue(response.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadShouldThrowWhenMissing() {
        when(notificationRepository.findByNotificationId("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead("missing", true));
    }

    @Test
    void markAllAsReadShouldMarkUnreadNotifications() {
        Notification first = notification("notification-101", "user-101", NotificationType.AI_DONE, NotificationChannel.APP, false);
        Notification second = notification("notification-102", "user-101", NotificationType.EXPORT_READY, NotificationChannel.APP, false);

        when(notificationRepository.findByRecipientIdAndIsReadOrderBySentAtDesc("user-101", false))
                .thenReturn(List.of(first, second));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.markAllAsRead("user-101");

        assertTrue(first.getIsRead());
        assertTrue(second.getIsRead());
        verify(notificationRepository).save(first);
        verify(notificationRepository).save(second);
    }

    @Test
    void getUnreadCountShouldReturnUnreadCount() {
        when(authServiceClient.getUserById("user-101")).thenReturn(activeFreeUser);
        when(notificationRepository.countByRecipientIdAndIsRead("user-101", false)).thenReturn(4L);

        long count = notificationService.getUnreadCount("user-101");

        assertEquals(4L, count);
    }

    @Test
    void deleteNotificationShouldDeleteNotification() {
        Notification notification = notification("notification-101", "user-101", NotificationType.AI_DONE, NotificationChannel.APP, false);

        when(notificationRepository.findByNotificationId("notification-101")).thenReturn(Optional.of(notification));

        notificationService.deleteNotification("notification-101");

        verify(notificationRepository).delete(notification);
    }

    @Test
    void getAllNotificationsShouldReturnAllNotifications() {
        Notification first = notification("notification-101", "user-101", NotificationType.AI_DONE, NotificationChannel.APP, false);
        Notification second = notification("notification-102", "user-202", NotificationType.EXPORT_READY, NotificationChannel.EMAIL, true);

        when(notificationRepository.findAll()).thenReturn(List.of(first, second));

        List<NotificationResponse> responses = notificationService.getAllNotifications();

        assertEquals(2, responses.size());
        assertEquals("notification-101", responses.get(0).notificationId());
        assertEquals("notification-102", responses.get(1).notificationId());
    }

    private void mockSave() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getNotificationId() == null) {
                notification.prePersist();
            }
            return notification;
        });
    }

    private Notification notification(String notificationId,
                                      String recipientId,
                                      NotificationType type,
                                      NotificationChannel channel,
                                      Boolean isRead) {
        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle("Test notification");
        notification.setMessage("Test message");
        notification.setChannel(channel);
        notification.setRelatedId("related-101");
        notification.setRelatedType("RESUME");
        notification.setActionUrl("/action");
        notification.setIsRead(isRead);
        notification.setSentAt(LocalDateTime.now());
        return notification;
    }
}
