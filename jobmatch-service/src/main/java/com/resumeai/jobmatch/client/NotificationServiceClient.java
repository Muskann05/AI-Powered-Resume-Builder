package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${notification.service.name}")
public interface NotificationServiceClient {

    @PostMapping("/notifications")
    void sendNotification(@RequestBody NotificationRequest request);
}