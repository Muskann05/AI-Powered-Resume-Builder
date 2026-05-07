package com.resumeai.template.service;

import com.resumeai.template.client.AuthServiceClient;
import com.resumeai.template.dto.AuthUserResponse;
import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.TemplatePreviewResponse;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.dto.UpdateTemplateRequest;
import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.enums.TemplateCategory;
import com.resumeai.template.exception.BadRequestException;
import com.resumeai.template.exception.ResourceNotFoundException;
import com.resumeai.template.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final TemplateRepository templateRepository;
    private final AuthServiceClient authServiceClient;
    private final TemplatePreviewBuilder templatePreviewBuilder;

    public TemplateServiceImpl(TemplateRepository templateRepository,
                               AuthServiceClient authServiceClient,
                               TemplatePreviewBuilder templatePreviewBuilder) {
        this.templateRepository = templateRepository;
        this.authServiceClient = authServiceClient;
        this.templatePreviewBuilder = templatePreviewBuilder;
    }

    @Override
    public TemplateResponse createTemplate(String adminUserId, CreateTemplateRequest request) {
        log.info("Creating template name={} category={} premium={}",
                request.name(), request.category(), request.isPremium());
        validateAdmin(adminUserId);

        ResumeTemplate template = new ResumeTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
        template.setThumbnailUrl(request.thumbnailUrl());
        template.setHtmlLayout(request.htmlLayout());
        template.setCssStyles(request.cssStyles());
        template.setCategory(request.category());
        template.setIsPremium(request.isPremium());
        template.setIsActive(true);
        template.setUsageCount(0);

        TemplateResponse response = map(templateRepository.save(template));
        log.info("Created template templateId={} name={}", response.templateId(), response.name());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(String templateId) {
        log.debug("Fetching active template templateId={}", templateId);
        return map(getActiveTemplateEntity(templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse previewTemplate(String templateId) {
        log.debug("Preview requested for templateId={}", templateId);
        return map(getActiveTemplateEntity(templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public TemplatePreviewResponse previewTemplateHtml(String templateId) {
        ResumeTemplate template = getActiveTemplateEntity(templateId);
        log.debug("Preview HTML requested for templateId={}", templateId);
        return new TemplatePreviewResponse(
                template.getTemplateId(),
                template.getName(),
                template.getThumbnailUrl(),
                templatePreviewBuilder.buildPreviewHtml(template)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        log.debug("Fetching all active templates");
        return templateRepository.findByIsActive(true).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getFreeTemplates() {
        log.debug("Fetching free templates");
        return templateRepository.findByIsPremiumAndIsActiveTrue(false).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getPremiumTemplates() {
        log.debug("Fetching premium templates");
        return templateRepository.findByIsPremiumAndIsActiveTrue(true).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getByCategory(String category) {
        log.debug("Fetching templates by category={}", category);
        TemplateCategory templateCategory = TemplateCategory.valueOf(category.toUpperCase());
        return templateRepository.findByCategoryAndIsActiveTrue(templateCategory).stream().map(this::map).toList();
    }

    @Override
    public TemplateResponse updateTemplate(String adminUserId, String templateId, UpdateTemplateRequest request) {
        log.info("Updating template templateId={} name={} premium={} active={}",
                templateId, request.name(), request.isPremium(), request.isActive());
        validateAdmin(adminUserId);

        ResumeTemplate template = getTemplateEntity(templateId);
        template.setName(request.name());
        template.setDescription(request.description());
        template.setThumbnailUrl(request.thumbnailUrl());
        template.setHtmlLayout(request.htmlLayout());
        template.setCssStyles(request.cssStyles());
        template.setCategory(request.category());
        template.setIsPremium(request.isPremium());
        template.setIsActive(request.isActive());

        TemplateResponse response = map(templateRepository.save(template));
        log.info("Updated template templateId={}", response.templateId());
        return response;
    }

    @Override
    public TemplateResponse deactivateTemplate(String adminUserId, String templateId) {
        log.info("Deactivating template templateId={}", templateId);
        validateAdmin(adminUserId);

        ResumeTemplate template = getTemplateEntity(templateId);
        template.setIsActive(false);
        return map(templateRepository.save(template));
    }

    @Override
    public TemplateResponse activateTemplate(String adminUserId, String templateId) {
        log.info("Activating template templateId={}", templateId);
        validateAdmin(adminUserId);

        ResumeTemplate template = getTemplateEntity(templateId);
        template.setIsActive(true);
        return map(templateRepository.save(template));
    }

    @Override
    public TemplateResponse incrementUsage(String templateId) {
        ResumeTemplate template = getActiveTemplateEntity(templateId);
        template.setUsageCount(template.getUsageCount() + 1);
        log.debug("Incrementing template usage templateId={} usageCount={}",
                templateId, template.getUsageCount());
        return map(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getPopularTemplates() {
        log.debug("Fetching popular templates");
        return templateRepository.findAllByIsActiveTrueOrderByUsageCountDesc().stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplatesForAdmin() {
        return templateRepository.findAllByOrderByUsageCountDesc().stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse validateTemplateAccess(String templateId, String userId) {
        log.debug("Validating template access templateId={} userId={}", templateId, userId);
        ResumeTemplate template = getActiveTemplateEntity(templateId);
        if (!Boolean.TRUE.equals(template.getIsPremium())) {
            return map(template);
        }

        AuthUserResponse user = authServiceClient.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BadRequestException("User account is inactive");
        }
        if (!"PREMIUM".equalsIgnoreCase(user.subscriptionPlan())) {
            throw new BadRequestException("Premium template access is allowed only for premium users");
        }

        return map(template);
    }

    private void validateAdmin(String adminUserId) {
        if (adminUserId == null || adminUserId.isBlank()) {
            throw new BadRequestException("Admin user id is required");
        }

        log.debug("Validating admin access adminUserId={}", adminUserId);
        AuthUserResponse user = authServiceClient.getUserById(adminUserId);
        if (user == null) {
            throw new BadRequestException("Admin user not found");
        }
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BadRequestException("Admin account is inactive");
        }
        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            throw new BadRequestException("Only admin users can manage templates");
        }
    }

    private ResumeTemplate getTemplateEntity(String templateId) {
        log.debug("Loading template entity templateId={}", templateId);
        return templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + templateId));
    }

    private ResumeTemplate getActiveTemplateEntity(String templateId) {
        log.debug("Loading active template entity templateId={}", templateId);
        return templateRepository.findByTemplateIdAndIsActiveTrue(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Active template not found with id: " + templateId));
    }

    private TemplateResponse map(ResumeTemplate template) {
        return new TemplateResponse(
                template.getTemplateId(),
                template.getName(),
                template.getDescription(),
                template.getThumbnailUrl(),
                template.getHtmlLayout(),
                template.getCssStyles(),
                template.getCategory(),
                template.getIsPremium(),
                template.getIsActive(),
                template.getUsageCount(),
                template.getCreatedAt()
        );
    }
}
