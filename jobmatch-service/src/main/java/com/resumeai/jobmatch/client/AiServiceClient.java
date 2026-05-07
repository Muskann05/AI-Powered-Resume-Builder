package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.AiJobFitRequest;
import com.resumeai.jobmatch.dto.AiJobFitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${ai.service.name}")
public interface AiServiceClient {

    @PostMapping("/ai/job-fit")
    AiJobFitResponse analyzeJobFit(@RequestBody AiJobFitRequest request);
}