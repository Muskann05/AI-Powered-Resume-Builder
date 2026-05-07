package com.resumeai.notification.repository;

import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByNotificationId(String notificationId);

    List<Notification> findByRecipientIdOrderBySentAtDesc(String recipientId);

    List<Notification> findByRecipientIdAndIsReadOrderBySentAtDesc(String recipientId, Boolean isRead);

    long countByRecipientIdAndIsRead(String recipientId, Boolean isRead);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByRelatedId(String relatedId);
}