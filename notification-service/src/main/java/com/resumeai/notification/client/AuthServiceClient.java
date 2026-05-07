package com.resumeai.notification.client;

import com.resumeai.notification.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "${auth.service.name}")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId);

    @GetMapping("/auth/users")
    List<UserResponse> getAllUsers();

    @GetMapping("/auth/users/by-plan")
    List<UserResponse> getUsersBySubscriptionPlan(@RequestParam("subscriptionPlan") String subscriptionPlan);
}