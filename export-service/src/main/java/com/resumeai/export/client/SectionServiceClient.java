package com.resumeai.export.client;

import com.resumeai.export.dto.SectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "${section.service.name}")
public interface SectionServiceClient {

    @GetMapping("/sections/resume/{resumeId}")
    List<SectionResponse> getSectionsByResume(@PathVariable("resumeId") String resumeId);
}
