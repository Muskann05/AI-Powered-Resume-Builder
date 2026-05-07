package com.resumeai.export.client;

import com.resumeai.export.dto.ResumeSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${resume.service.name}")
public interface ResumeServiceClient {

    @GetMapping("/resumes/{resumeId}")
    ResumeSummaryResponse getResumeById(@PathVariable("resumeId") String resumeId);
}
