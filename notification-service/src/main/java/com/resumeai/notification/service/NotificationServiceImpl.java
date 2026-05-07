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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final AuthServiceClient authServiceClient;
    private final JavaMailSender javaMailSender;

    @Value("${notification.mail.from}")
    private String fromEmail;

    @Value("${notification.email.provider:smtp}")
    private String emailProvider;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   AuthServiceClient authServiceClient,
                                   JavaMailSender javaMailSender) {
        this.notificationRepository = notificationRepository;
        this.authServiceClient = authServiceClient;
        this.javaMailSender = javaMailSender;
    }

    @Override
    public NotificationResponse sendNotification(CreateNotificationRequest request) {
        log.info("Sending notification recipientId={} type={} channel={}",
                request.recipientId(), request.type(), request.channel());
        UserResponse user = validateUser(request.recipientId());
        NotificationChannel channel = resolveChannel(request.channel());
        NotificationType type = resolveType(request.type());

        Notification notification = new Notification();
        notification.setRecipientId(request.recipientId());
        notification.setType(type);
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setChannel(channel);
        notification.setRelatedId(request.relatedId());
        notification.setRelatedType(request.relatedType());
        notification.setActionUrl(request.actionUrl());

        Notification saved = notificationRepository.save(notification);

        if (channel == NotificationChannel.EMAIL) {
            trySendEmail(user, saved);
        }

        log.info("Notification persisted notificationId={}", saved.getNotificationId());
        return map(saved);
    }

    @Override
    public List<NotificationResponse> sendBulkNotification(BulkNotificationRequest request) {
        log.info("Sending bulk notification subscriptionPlan={} type={}",
                request.subscriptionPlan(), request.type());
        List<UserResponse> users = resolveBulkRecipients(request.subscriptionPlan());
        List<NotificationResponse> responses = new ArrayList<>();

        for (UserResponse user : users) {
            if (!Boolean.TRUE.equals(user.isActive())) {
                continue;
            }
            responses.add(sendNotification(new CreateNotificationRequest(
                    user.userId(),
                    request.type(),
                    request.title(),
                    request.message(),
                    request.channel(),
                    request.relatedId(),
                    request.relatedType(),
                    request.actionUrl()
            )));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByRecipient(String recipientId) {
        validateUser(recipientId);
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(String recipientId) {
        validateUser(recipientId);
        return notificationRepository.findByRecipientIdAndIsReadOrderBySentAtDesc(recipientId, false)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(String notificationId, Boolean isRead) {
        log.debug("Updating notification read state notificationId={} isRead={}", notificationId, isRead);
        Notification notification = getNotificationEntity(notificationId);
        notification.setIsRead(isRead);
        return map(notificationRepository.save(notification));
    }

    @Override
    public void markAllAsRead(String recipientId) {
        log.info("Marking all notifications as read recipientId={}", recipientId);
        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsReadOrderBySentAtDesc(recipientId, false);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientId) {
        validateUser(recipientId);
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public void deleteNotification(String notificationId) {
        log.info("Deleting notification notificationId={}", notificationId);
        notificationRepository.delete(getNotificationEntity(notificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream().map(this::map).toList();
    }

    private UserResponse validateUser(String userId) {
        log.debug("Validating notification user userId={}", userId);
        UserResponse user = authServiceClient.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        return user;
    }

    private List<UserResponse> resolveBulkRecipients(String subscriptionPlan) {
        if (subscriptionPlan == null || subscriptionPlan.isBlank()) {
            return authServiceClient.getAllUsers();
        }
        return authServiceClient.getUsersBySubscriptionPlan(subscriptionPlan.toUpperCase());
    }

    private Notification getNotificationEntity(String notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
    }

    private NotificationType resolveType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid notification type: " + type);
        }
    }

    private NotificationChannel resolveChannel(String channel) {
        try {
            return NotificationChannel.valueOf(channel.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid notification channel: " + channel);
        }
    }

    private void trySendEmail(UserResponse user, Notification notification) {
        if (!emailEnabled) {
            log.info("Email delivery disabled, skipping email for notificationId={}", notification.getNotificationId());
            return;
        }

        if (user.email() == null || user.email().isBlank()) {
            log.warn("Recipient email missing for notificationId={}", notification.getNotificationId());
            return;
        }

        if ("log".equalsIgnoreCase(emailProvider)) {
            log.info("Dummy email delivery to={} subject={} body={}",
                    user.email(), notification.getTitle(), notification.getMessage());
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(user.email());
            mail.setSubject(notification.getTitle());
            mail.setText(notification.getMessage() + buildActionLine(notification.getActionUrl()));
            javaMailSender.send(mail);
            log.info("SMTP email sent to={} notificationId={}", user.email(), notification.getNotificationId());
        } catch (Exception ex) {
            log.warn("Email delivery failed to={} notificationId={} reason={}",
                    user.email(), notification.getNotificationId(), ex.getMessage());
        }
    }

    private String buildActionLine(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return "";
        }
        return "\n\nAction: " + actionUrl;
    }

    private NotificationResponse map(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getRecipientId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getChannel().name(),
                notification.getRelatedId(),
                notification.getRelatedType(),
                notification.getActionUrl(),
                notification.getIsRead(),
                notification.getSentAt()
        );
    }
}
