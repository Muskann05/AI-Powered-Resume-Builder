package com.resumeai.ai.client;

import com.resumeai.ai.dto.ResumeSummaryResponse;
import com.resumeai.ai.dto.UpdateAtsScoreRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${resume.service.name}")
public interface ResumeServiceClient {

    @GetMapping("/resumes/{resumeId}")
    ResumeSummaryResponse getResumeById(@PathVariable("resumeId") String resumeId);

    @PutMapping("/resumes/{resumeId}/ats-score")
    ResumeSummaryResponse updateAtsScore(@PathVariable("resumeId") String resumeId,
                                         @RequestBody UpdateAtsScoreRequest request);
}
