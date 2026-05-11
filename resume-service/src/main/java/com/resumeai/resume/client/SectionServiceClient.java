package com.resumeai.resume.client;

import com.resumeai.resume.dto.SectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "section-service", url = "${section.service.url}")
public interface SectionServiceClient {

    @GetMapping("/sections/resume/{resumeId}")
    List<SectionResponse> getSectionsByResume(@PathVariable("resumeId") String resumeId);

    @DeleteMapping("/sections/resume/{resumeId}")
    void deleteAllSections(@PathVariable("resumeId") String resumeId);

    @PostMapping("/sections/resume/{sourceResumeId}/clone/{targetResumeId}")
    void cloneSections(@PathVariable("sourceResumeId") String sourceResumeId,
                       @PathVariable("targetResumeId") String targetResumeId);
}
