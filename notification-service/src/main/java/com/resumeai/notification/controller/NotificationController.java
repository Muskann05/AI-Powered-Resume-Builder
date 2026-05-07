package com.resumeai.notification.controller;

import com.resumeai.notification.dto.ApiMessageResponse;
import com.resumeai.notification.dto.BulkNotificationRequest;
import com.resumeai.notification.dto.CreateNotificationRequest;
import com.resumeai.notification.dto.MarkReadRequest;
import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.UnreadCountResponse;
import com.resumeai.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponse>> sendBulkNotification(@Valid @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendBulkNotification(request));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByRecipient(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable String recipientId) {
        return ResponseEntity.ok(new UnreadCountResponse(recipientId, notificationService.getUnreadCount(recipientId)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable String notificationId,
                                                           @Valid @RequestBody MarkReadRequest request) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, request.isRead()));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<ApiMessageResponse> markAllRead(@PathVariable String recipientId) {
        notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(new ApiMessageResponse("All notifications marked as read"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiMessageResponse> deleteNotification(@PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(new ApiMessageResponse("Notification deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
}