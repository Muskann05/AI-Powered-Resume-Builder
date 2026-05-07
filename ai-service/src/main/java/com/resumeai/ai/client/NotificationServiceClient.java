package com.resumeai.ai.client;

import com.resumeai.ai.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${notification.service.name}")
public interface NotificationServiceClient {

    @PostMapping("/notifications")
    Object sendNotification(@RequestBody NotificationRequest request);
}
