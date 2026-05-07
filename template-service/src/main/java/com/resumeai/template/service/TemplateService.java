package com.resumeai.template.service;

import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.TemplatePreviewResponse;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.dto.UpdateTemplateRequest;

import java.util.List;

public interface TemplateService {

    TemplateResponse createTemplate(String adminUserId, CreateTemplateRequest request);

    TemplateResponse getTemplateById(String templateId);

    TemplateResponse previewTemplate(String templateId);

    TemplatePreviewResponse previewTemplateHtml(String templateId);

    List<TemplateResponse> getAllTemplates();

    List<TemplateResponse> getFreeTemplates();

    List<TemplateResponse> getPremiumTemplates();

    List<TemplateResponse> getByCategory(String category);

    TemplateResponse updateTemplate(String adminUserId, String templateId, UpdateTemplateRequest request);

    TemplateResponse deactivateTemplate(String adminUserId, String templateId);

    TemplateResponse activateTemplate(String adminUserId, String templateId);

    TemplateResponse incrementUsage(String templateId);

    List<TemplateResponse> getPopularTemplates();

    List<TemplateResponse> getAllTemplatesForAdmin();

    TemplateResponse validateTemplateAccess(String templateId, String userId);
}