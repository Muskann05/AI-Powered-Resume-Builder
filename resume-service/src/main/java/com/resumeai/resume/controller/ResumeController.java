package com.resumeai.resume.controller;

import com.resumeai.resume.dto.ApiMessageResponse;
import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeAdminStatsResponse;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.UpdateAtsScoreRequest;
import com.resumeai.resume.dto.UpdateResumeRequest;
import com.resumeai.resume.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(@Valid @RequestBody CreateResumeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.createResume(request));
    }

    @PostMapping("/{resumeId}/duplicate")
    public ResponseEntity<ResumeResponse> duplicateResume(@PathVariable String resumeId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.duplicateResume(resumeId));
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<ResumeResponse> getResumeById(@PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.getResumeById(resumeId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByUser(@PathVariable String userId) {
        return ResponseEntity.ok(resumeService.getResumesByUser(userId));
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByTemplate(@PathVariable String templateId) {
        return ResponseEntity.ok(resumeService.getResumesByTemplate(templateId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<ResumeResponse>> getAllResumesForAdmin() {
        return ResponseEntity.ok(resumeService.getAllResumesForAdmin());
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<ResumeAdminStatsResponse> getAdminStats() {
        return ResponseEntity.ok(resumeService.getAdminStats());
    }

    @DeleteMapping("/admin/{resumeId}")
    public ResponseEntity<ApiMessageResponse> deleteResumeForAdmin(@PathVariable String resumeId) {
        resumeService.deleteResume(resumeId);
        return ResponseEntity.ok(new ApiMessageResponse("Resume deleted by admin successfully"));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ResumeResponse>> getPublicResumes() {
        return ResponseEntity.ok(resumeService.getPublicResumes());
    }

    @GetMapping("/public/search")
    public ResponseEntity<List<ResumeResponse>> searchPublicResumes(@RequestParam String keyword) {
        return ResponseEntity.ok(resumeService.searchPublicResumes(keyword));
    }

    @GetMapping("/public/{resumeId}")
    public ResponseEntity<ResumeResponse> getPublicResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.getPublicResume(resumeId));
    }

    @PutMapping("/{resumeId}")
    public ResponseEntity<ResumeResponse> updateResume(@PathVariable String resumeId,
                                                       @Valid @RequestBody UpdateResumeRequest request) {
        return ResponseEntity.ok(resumeService.updateResume(resumeId, request));
    }

    @PutMapping("/{resumeId}/publish")
    public ResponseEntity<ResumeResponse> publishResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.publishResume(resumeId));
    }

    @PutMapping("/{resumeId}/unpublish")
    public ResponseEntity<ResumeResponse> unpublishResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.unpublishResume(resumeId));
    }

    @PutMapping("/{resumeId}/ats-score")
    public ResponseEntity<ResumeResponse> updateAtsScore(@PathVariable String resumeId,
                                                         @Valid @RequestBody UpdateAtsScoreRequest request) {
        return ResponseEntity.ok(resumeService.updateAtsScore(resumeId, request.atsScore()));
    }

    @PutMapping("/{resumeId}/view")
    public ResponseEntity<ResumeResponse> incrementViewCount(@PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.incrementViewCount(resumeId));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<ApiMessageResponse> deleteResume(@PathVariable String resumeId) {
        resumeService.deleteResume(resumeId);
        return ResponseEntity.ok(new ApiMessageResponse("Resume deleted successfully"));
    }
}
