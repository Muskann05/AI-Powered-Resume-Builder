package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.ResumeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${resume.service.name}")
public interface ResumeServiceClient {

    @GetMapping("/resumes/{resumeId}")
    ResumeResponse getResumeById(@PathVariable("resumeId") String resumeId);
}