package com.resumeai.notification.messaging;

import com.resumeai.notification.config.RabbitMqConfig;
import com.resumeai.notification.dto.CreateNotificationRequest;
import com.resumeai.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.EXPORT_READY_QUEUE)
    public void handleExportReady(ExportReadyMessage message) {
        log.info("Received export ready event jobId={} userId={}", message.getJobId(), message.getUserId());
        notificationService.sendNotification(new CreateNotificationRequest(
                message.getUserId(),
                "EXPORT_READY",
                "Export ready",
                "Your export is ready for download.",
                "APP",
                message.getResumeId(),
                "RESUME",
                "/exports/" + message.getJobId()
        ));
    }

    @RabbitListener(queues = RabbitMqConfig.PLAN_CHANGED_QUEUE)
    public void handlePlanChanged(PlanChangedEvent event) {
        log.info("Received plan changed event userId={} oldPlan={} newPlan={}",
                event.getUserId(), event.getOldPlan(), event.getNewPlan());
        notificationService.sendNotification(new CreateNotificationRequest(
                event.getUserId(),
                "PLAN_CHANGE",
                "Subscription updated",
                "Your plan has changed from " + event.getOldPlan() + " to " + event.getNewPlan(),
                "APP",
                event.getUserId(),
                "USER",
                "/subscription"
        ));
    }
}
