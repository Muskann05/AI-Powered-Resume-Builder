package com.resumeai.resume.client;

import com.resumeai.resume.dto.TemplateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "template-service", url = "${template.service.url}")
public interface TemplateServiceClient {

    @GetMapping("/templates/{templateId}/validate-access/{userId}")
    TemplateResponse validateTemplateAccess(@PathVariable("templateId") String templateId,
                                            @PathVariable("userId") String userId);

    @PutMapping("/templates/{templateId}/usage")
    TemplateResponse incrementUsage(@PathVariable("templateId") String templateId);
}
