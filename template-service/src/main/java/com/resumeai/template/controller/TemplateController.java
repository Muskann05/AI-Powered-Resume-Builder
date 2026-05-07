package com.resumeai.template.controller;

import com.resumeai.template.dto.ApiMessageResponse;
import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.TemplatePreviewResponse;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.dto.UpdateTemplateRequest;
import com.resumeai.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@RequestParam String adminUserId,
                                                           @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(adminUserId, request));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<TemplateResponse>> getAllTemplatesForAdmin() {
        return ResponseEntity.ok(templateService.getAllTemplatesForAdmin());
    }

    @GetMapping("/free")
    public ResponseEntity<List<TemplateResponse>> getFreeTemplates() {
        return ResponseEntity.ok(templateService.getFreeTemplates());
    }

    @GetMapping("/premium")
    public ResponseEntity<List<TemplateResponse>> getPremiumTemplates() {
        return ResponseEntity.ok(templateService.getPremiumTemplates());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<TemplateResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(templateService.getByCategory(category));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<TemplateResponse>> getPopularTemplates() {
        return ResponseEntity.ok(templateService.getPopularTemplates());
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable String templateId) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @GetMapping("/{templateId}/preview")
    public ResponseEntity<TemplateResponse> previewTemplate(@PathVariable String templateId) {
        return ResponseEntity.ok(templateService.previewTemplate(templateId));
    }

    @GetMapping(value = "/{templateId}/preview/html", produces = "text/html")
    public ResponseEntity<String> previewTemplateHtml(@PathVariable String templateId) {
        TemplatePreviewResponse preview = templateService.previewTemplateHtml(templateId);
        return ResponseEntity.ok(preview.previewHtml());
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> updateTemplate(@RequestParam String adminUserId,
                                                           @PathVariable String templateId,
                                                           @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(adminUserId, templateId, request));
    }

    @PutMapping("/{templateId}/deactivate")
    public ResponseEntity<TemplateResponse> deactivateTemplate(@RequestParam String adminUserId,
                                                               @PathVariable String templateId) {
        return ResponseEntity.ok(templateService.deactivateTemplate(adminUserId, templateId));
    }

    @PutMapping("/{templateId}/activate")
    public ResponseEntity<TemplateResponse> activateTemplate(@RequestParam String adminUserId,
                                                             @PathVariable String templateId) {
        return ResponseEntity.ok(templateService.activateTemplate(adminUserId, templateId));
    }

    @PutMapping("/{templateId}/usage")
    public ResponseEntity<TemplateResponse> incrementUsage(@PathVariable String templateId) {
        return ResponseEntity.ok(templateService.incrementUsage(templateId));
    }

    @GetMapping("/{templateId}/validate-access/{userId}")
    public ResponseEntity<TemplateResponse> validateTemplateAccess(@PathVariable String templateId,
                                                                   @PathVariable String userId) {
        return ResponseEntity.ok(templateService.validateTemplateAccess(templateId, userId));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiMessageResponse> deleteNote(@PathVariable String templateId) {
        return ResponseEntity.ok(new ApiMessageResponse("Use deactivate endpoint instead of physical delete"));
    }
}
