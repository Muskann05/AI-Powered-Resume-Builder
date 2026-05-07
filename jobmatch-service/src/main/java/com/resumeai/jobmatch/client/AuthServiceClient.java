package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${auth.service.name}")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId);
}