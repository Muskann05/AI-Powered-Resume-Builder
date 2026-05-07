package com.resumeai.export.client;

import com.resumeai.export.dto.TemplateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${template.service.name}")
public interface TemplateServiceClient {

    @GetMapping("/templates/{templateId}/validate-access/{userId}")
    TemplateResponse validateTemplateAccess(@PathVariable("templateId") String templateId,
                                            @PathVariable("userId") String userId);
}
