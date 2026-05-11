package com.resumeai.resume.client;

import com.resumeai.resume.dto.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    AuthUserResponse getUserById(@PathVariable("userId") String userId);
}
