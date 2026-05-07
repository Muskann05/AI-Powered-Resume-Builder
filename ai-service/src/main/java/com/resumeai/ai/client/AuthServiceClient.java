package com.resumeai.ai.client;

import com.resumeai.ai.dto.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${auth.service.name}")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    AuthUserResponse getUserById(@PathVariable("userId") String userId);
}
