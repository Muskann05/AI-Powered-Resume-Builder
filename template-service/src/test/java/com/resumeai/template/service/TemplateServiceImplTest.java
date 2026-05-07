package com.resumeai.template.service;

import com.resumeai.template.client.AuthServiceClient;
import com.resumeai.template.dto.AuthUserResponse;
import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.UpdateTemplateRequest;
import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.enums.TemplateCategory;
import com.resumeai.template.exception.BadRequestException;
import com.resumeai.template.exception.ResourceNotFoundException;
import com.resumeai.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private TemplatePreviewBuilder templatePreviewBuilder;

    private TemplateServiceImpl templateService;
    private ResumeTemplate template;
    private ResumeTemplate premiumTemplate;
    private AuthUserResponse adminUser;
    private AuthUserResponse normalUser;
    private AuthUserResponse premiumUser;

    @BeforeEach
    void setUp() {
        templateService = new TemplateServiceImpl(
                templateRepository,
                authServiceClient,
                templatePreviewBuilder
        );

        template = new ResumeTemplate();
        template.setTemplateId("template-101");
        template.setName("Modern");
        template.setDescription("Modern resume template");
        template.setThumbnailUrl("modern.png");
        template.setHtmlLayout("<html>{{sections}}</html>");
        template.setCssStyles("body{}");
        template.setCategory(TemplateCategory.MODERN);
        template.setIsPremium(false);
        template.setIsActive(true);
        template.setUsageCount(5);

        premiumTemplate = new ResumeTemplate();
        premiumTemplate.setTemplateId("template-premium");
        premiumTemplate.setName("Premium Pro");
        premiumTemplate.setDescription("Premium resume template");
        premiumTemplate.setThumbnailUrl("premium.png");
        premiumTemplate.setHtmlLayout("<html>{{sections}}</html>");
        premiumTemplate.setCssStyles("body{}");
        premiumTemplate.setCategory(TemplateCategory.PROFESSIONAL);
        premiumTemplate.setIsPremium(true);
        premiumTemplate.setIsActive(true);
        premiumTemplate.setUsageCount(10);

        adminUser = new AuthUserResponse(
                "admin-101",
                "Admin",
                "admin@example.com",
                "9999999999",
                "ADMIN",
                "LOCAL",
                true,
                "PREMIUM",
                null
        );

        normalUser = new AuthUserResponse(
                "user-101",
                "Muskan",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "FREE",
                null
        );

        premiumUser = new AuthUserResponse(
                "user-202",
                "Premium User",
                "premium@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "PREMIUM",
                null
        );
    }

    @Test
    void createTemplateShouldCreateTemplateForAdmin() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Creative",
                "Creative resume template",
                "creative.png",
                "<html></html>",
                "body{}",
                TemplateCategory.CREATIVE,
                false
        );

        when(authServiceClient.getUserById("admin-101")).thenReturn(adminUser);
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(invocation -> {
            ResumeTemplate saved = invocation.getArgument(0);
            saved.setTemplateId("template-new");
            return saved;
        });

        var response = templateService.createTemplate("admin-101", request);

        assertEquals("template-new", response.templateId());
        assertEquals("Creative", response.name());
        assertEquals(TemplateCategory.CREATIVE, response.category());
        assertFalse(response.isPremium());
        assertTrue(response.isActive());
        assertEquals(0, response.usageCount());

        verify(templateRepository).save(any(ResumeTemplate.class));
    }

    @Test
    void createTemplateShouldRejectBlankAdminId() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Creative",
                "Creative resume template",
                "creative.png",
                "<html></html>",
                "body{}",
                TemplateCategory.CREATIVE,
                false
        );

        assertThrows(BadRequestException.class, () -> templateService.createTemplate("", request));
    }

    @Test
    void createTemplateShouldRejectNonAdminUser() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Creative",
                "Creative resume template",
                "creative.png",
                "<html></html>",
                "body{}",
                TemplateCategory.CREATIVE,
                false
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(normalUser);

        assertThrows(BadRequestException.class, () -> templateService.createTemplate("user-101", request));
    }

    @Test
    void createTemplateShouldRejectInactiveAdmin() {
        AuthUserResponse inactiveAdmin = new AuthUserResponse(
                "admin-101",
                "Admin",
                "admin@example.com",
                "9999999999",
                "ADMIN",
                "LOCAL",
                false,
                "PREMIUM",
                null
        );

        CreateTemplateRequest request = new CreateTemplateRequest(
                "Creative",
                "Creative resume template",
                "creative.png",
                "<html></html>",
                "body{}",
                TemplateCategory.CREATIVE,
                false
        );

        when(authServiceClient.getUserById("admin-101")).thenReturn(inactiveAdmin);

        assertThrows(BadRequestException.class, () -> templateService.createTemplate("admin-101", request));
    }

    @Test
    void getTemplateByIdShouldReturnActiveTemplate() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-101")).thenReturn(Optional.of(template));

        var response = templateService.getTemplateById("template-101");

        assertEquals("template-101", response.templateId());
        assertEquals("Modern", response.name());
    }

    @Test
    void getTemplateByIdShouldThrowWhenActiveTemplateMissing() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("missing-template")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getTemplateById("missing-template"));
    }

    @Test
    void previewTemplateShouldReturnTemplate() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-101")).thenReturn(Optional.of(template));

        var response = templateService.previewTemplate("template-101");

        assertEquals("template-101", response.templateId());
        assertEquals("<html>{{sections}}</html>", response.htmlLayout());
    }

    @Test
    void previewTemplateHtmlShouldBuildPreviewHtml() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-101")).thenReturn(Optional.of(template));
        when(templatePreviewBuilder.buildPreviewHtml(template)).thenReturn("<html><body>Preview</body></html>");

        var response = templateService.previewTemplateHtml("template-101");

        assertEquals("template-101", response.templateId());
        assertEquals("Modern", response.name());
        assertEquals("<html><body>Preview</body></html>", response.previewHtml());
    }

    @Test
    void getAllTemplatesShouldReturnActiveTemplates() {
        when(templateRepository.findByIsActive(true)).thenReturn(List.of(template));

        var response = templateService.getAllTemplates();

        assertEquals(1, response.size());
        assertEquals("template-101", response.get(0).templateId());
    }

    @Test
    void getFreeTemplatesShouldReturnOnlyFreeActiveTemplates() {
        when(templateRepository.findByIsPremiumAndIsActiveTrue(false)).thenReturn(List.of(template));

        var response = templateService.getFreeTemplates();

        assertEquals(1, response.size());
        assertFalse(response.get(0).isPremium());
    }

    @Test
    void getPremiumTemplatesShouldReturnOnlyPremiumActiveTemplates() {
        when(templateRepository.findByIsPremiumAndIsActiveTrue(true)).thenReturn(List.of(premiumTemplate));

        var response = templateService.getPremiumTemplates();

        assertEquals(1, response.size());
        assertTrue(response.get(0).isPremium());
    }

    @Test
    void getByCategoryShouldReturnActiveTemplatesByCategory() {
        when(templateRepository.findByCategoryAndIsActiveTrue(TemplateCategory.MODERN)).thenReturn(List.of(template));

        var response = templateService.getByCategory("modern");

        assertEquals(1, response.size());
        assertEquals(TemplateCategory.MODERN, response.get(0).category());
    }

    @Test
    void getByCategoryShouldRejectInvalidCategory() {
        assertThrows(IllegalArgumentException.class, () -> templateService.getByCategory("invalid-category"));
    }

    @Test
    void updateTemplateShouldUpdateTemplateForAdmin() {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "Modern Updated",
                "Updated description",
                "updated.png",
                "<html>updated</html>",
                "body{color:black}",
                TemplateCategory.MINIMALIST,
                true,
                true
        );

        when(authServiceClient.getUserById("admin-101")).thenReturn(adminUser);
        when(templateRepository.findByTemplateId("template-101")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = templateService.updateTemplate("admin-101", "template-101", request);

        assertEquals("Modern Updated", response.name());
        assertEquals("Updated description", response.description());
        assertEquals("updated.png", response.thumbnailUrl());
        assertEquals(TemplateCategory.MINIMALIST, response.category());
        assertTrue(response.isPremium());
        assertTrue(response.isActive());
    }

    @Test
    void updateTemplateShouldThrowWhenTemplateMissing() {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "Modern Updated",
                "Updated description",
                "updated.png",
                "<html>updated</html>",
                "body{color:black}",
                TemplateCategory.MINIMALIST,
                true,
                true
        );

        when(authServiceClient.getUserById("admin-101")).thenReturn(adminUser);
        when(templateRepository.findByTemplateId("missing-template")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> templateService.updateTemplate("admin-101", "missing-template", request));
    }

    @Test
    void deactivateTemplateShouldMarkInactive() {
        when(authServiceClient.getUserById("admin-101")).thenReturn(adminUser);
        when(templateRepository.findByTemplateId("template-101")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = templateService.deactivateTemplate("admin-101", "template-101");

        assertFalse(response.isActive());
    }

    @Test
    void activateTemplateShouldMarkActive() {
        template.setIsActive(false);

        when(authServiceClient.getUserById("admin-101")).thenReturn(adminUser);
        when(templateRepository.findByTemplateId("template-101")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = templateService.activateTemplate("admin-101", "template-101");

        assertTrue(response.isActive());
    }

    @Test
    void incrementUsageShouldIncreaseUsageCount() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-101")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = templateService.incrementUsage("template-101");

        assertEquals(6, response.usageCount());
    }

    @Test
    void incrementUsageShouldThrowWhenTemplateMissing() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("missing-template")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.incrementUsage("missing-template"));
    }

    @Test
    void getPopularTemplatesShouldReturnActiveTemplatesOrderedByUsage() {
        when(templateRepository.findAllByIsActiveTrueOrderByUsageCountDesc()).thenReturn(List.of(premiumTemplate, template));

        var response = templateService.getPopularTemplates();

        assertEquals(2, response.size());
        assertEquals("template-premium", response.get(0).templateId());
    }

    @Test
    void getAllTemplatesForAdminShouldReturnAllTemplatesOrderedByUsage() {
        when(templateRepository.findAllByOrderByUsageCountDesc()).thenReturn(List.of(premiumTemplate, template));

        var response = templateService.getAllTemplatesForAdmin();

        assertEquals(2, response.size());
        assertEquals("template-premium", response.get(0).templateId());
        assertEquals("template-101", response.get(1).templateId());
    }

    @Test
    void validateTemplateAccessShouldAllowFreeTemplateForAnyActiveUser() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-101")).thenReturn(Optional.of(template));

        var response = templateService.validateTemplateAccess("template-101", "user-101");

        assertEquals("template-101", response.templateId());
        assertFalse(response.isPremium());
    }

    @Test
    void validateTemplateAccessShouldAllowPremiumTemplateForPremiumUser() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-premium")).thenReturn(Optional.of(premiumTemplate));
        when(authServiceClient.getUserById("user-202")).thenReturn(premiumUser);

        var response = templateService.validateTemplateAccess("template-premium", "user-202");

        assertEquals("template-premium", response.templateId());
        assertTrue(response.isPremium());
    }

    @Test
    void validateTemplateAccessShouldRejectPremiumTemplateForFreeUser() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-premium")).thenReturn(Optional.of(premiumTemplate));
        when(authServiceClient.getUserById("user-101")).thenReturn(normalUser);

        assertThrows(BadRequestException.class,
                () -> templateService.validateTemplateAccess("template-premium", "user-101"));
    }

    @Test
    void validateTemplateAccessShouldRejectInactiveUserForPremiumTemplate() {
        AuthUserResponse inactiveUser = new AuthUserResponse(
                "user-101",
                "Muskan",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                false,
                "PREMIUM",
                null
        );

        when(templateRepository.findByTemplateIdAndIsActiveTrue("template-premium")).thenReturn(Optional.of(premiumTemplate));
        when(authServiceClient.getUserById("user-101")).thenReturn(inactiveUser);

        assertThrows(BadRequestException.class,
                () -> templateService.validateTemplateAccess("template-premium", "user-101"));
    }

    @Test
    void validateTemplateAccessShouldThrowWhenTemplateMissing() {
        when(templateRepository.findByTemplateIdAndIsActiveTrue("missing-template")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> templateService.validateTemplateAccess("missing-template", "user-101"));
    }
}
