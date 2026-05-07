package com.resumeai.notification.entity;

import com.resumeai.notification.enums.NotificationChannel;
import com.resumeai.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "notification_id", nullable = false, updatable = false, length = 36)
    private String notificationId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "related_id", length = 36)
    private String relatedId;

    @Column(name = "related_type", length = 50)
    private String relatedType;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        if (notificationId == null) notificationId = UUID.randomUUID().toString();
        if (sentAt == null) sentAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
        if (channel == null) channel = NotificationChannel.APP;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean read) { isRead = read; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}