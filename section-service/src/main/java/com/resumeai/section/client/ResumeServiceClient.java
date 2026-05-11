package com.resumeai.section.client;

import com.resumeai.section.dto.ResumeSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "resume-service", url = "${resume.service.url}")
public interface ResumeServiceClient {

    @GetMapping("/resumes/{resumeId}")
    ResumeSummaryResponse getResumeById(@PathVariable("resumeId") String resumeId);
}
