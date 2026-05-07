package com.resumeai.section.controller;

import com.resumeai.section.dto.ApiMessageResponse;
import com.resumeai.section.dto.BulkUpdateSectionsRequest;
import com.resumeai.section.dto.CreateSectionRequest;
import com.resumeai.section.dto.ReorderSectionsRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.ToggleVisibilityRequest;
import com.resumeai.section.dto.UpdateSectionRequest;
import com.resumeai.section.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping
    public ResponseEntity<SectionResponse> addSection(@Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.addSection(request));
    }

    @PostMapping("/resume/{sourceResumeId}/clone/{targetResumeId}")
    public ResponseEntity<ApiMessageResponse> cloneSections(@PathVariable String sourceResumeId,
                                                            @PathVariable String targetResumeId) {
        sectionService.cloneSections(sourceResumeId, targetResumeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse("Sections cloned successfully"));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<List<SectionResponse>> getSectionsByResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(sectionService.getSectionsByResume(resumeId));
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> getSectionById(@PathVariable String sectionId) {
        return ResponseEntity.ok(sectionService.getSectionById(sectionId));
    }

    @GetMapping("/resume/{resumeId}/type/{sectionType}")
    public ResponseEntity<List<SectionResponse>> getSectionsByType(@PathVariable String resumeId,
                                                                   @PathVariable String sectionType) {
        return ResponseEntity.ok(sectionService.getSectionsByType(resumeId, sectionType));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> updateSection(@PathVariable String sectionId,
                                                         @Valid @RequestBody UpdateSectionRequest request) {
        return ResponseEntity.ok(sectionService.updateSection(sectionId, request));
    }

    @PutMapping("/resume/{resumeId}/reorder")
    public ResponseEntity<List<SectionResponse>> reorderSections(@PathVariable String resumeId,
                                                                 @RequestBody List<@Valid ReorderSectionsRequest> request) {
        return ResponseEntity.ok(sectionService.reorderSections(resumeId, request));
    }

    @PutMapping("/{sectionId}/visibility")
    public ResponseEntity<SectionResponse> toggleVisibility(@PathVariable String sectionId,
                                                            @Valid @RequestBody ToggleVisibilityRequest request) {
        return ResponseEntity.ok(sectionService.toggleVisibility(sectionId, request.isVisible()));
    }

    @PutMapping("/bulk")
    public ResponseEntity<List<SectionResponse>> bulkUpdateSections(@Valid @RequestBody BulkUpdateSectionsRequest request) {
        return ResponseEntity.ok(sectionService.bulkUpdateSections(request));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<ApiMessageResponse> deleteSection(@PathVariable String sectionId) {
        sectionService.deleteSection(sectionId);
        return ResponseEntity.ok(new ApiMessageResponse("Section deleted successfully"));
    }

    @DeleteMapping("/resume/{resumeId}")
    public ResponseEntity<ApiMessageResponse> deleteAllSections(@PathVariable String resumeId) {
        sectionService.deleteAllSections(resumeId);
        return ResponseEntity.ok(new ApiMessageResponse("All sections deleted successfully"));
    }
}
