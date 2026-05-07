package com.resumeai.notification.service;

import com.resumeai.notification.dto.BulkNotificationRequest;
import com.resumeai.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(com.resumeai.notification.dto.CreateNotificationRequest request);

    List<NotificationResponse> sendBulkNotification(BulkNotificationRequest request);

    List<NotificationResponse> getNotificationsByRecipient(String recipientId);

    List<NotificationResponse> getUnreadNotifications(String recipientId);

    NotificationResponse markAsRead(String notificationId, Boolean isRead);

    void markAllAsRead(String recipientId);

    long getUnreadCount(String recipientId);

    void deleteNotification(String notificationId);

    List<NotificationResponse> getAllNotifications();
}