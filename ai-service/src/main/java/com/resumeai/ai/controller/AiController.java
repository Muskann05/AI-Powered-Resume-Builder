package com.resumeai.ai.controller;

import com.resumeai.ai.dto.*;
import com.resumeai.ai.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate-summary")
    public AiResponseDto generateSummary(@Valid @RequestBody SummaryRequest request) {
        return aiService.generateSummary(request, false);
    }

    @PostMapping("/generate-bullets")
    public AiResponseDto generateBullets(@Valid @RequestBody BulletPointsRequest request) {
        return aiService.generateBulletPoints(request, false);
    }

    @PostMapping("/generate-cover-letter")
    public AiResponseDto generateCoverLetter(@Valid @RequestBody CoverLetterRequest request) {
        return aiService.generateCoverLetter(request, false);
    }

    @PostMapping("/improve-section")
    public AiResponseDto improveSection(@Valid @RequestBody ImproveSectionRequest request) {
        return aiService.improveSection(request, false);
    }

    @PostMapping("/check-ats")
    public AtsCheckResponse checkAts(@Valid @RequestBody AtsCheckRequest request) {
        return aiService.checkAtsCompatibility(request, false);
    }

    @PostMapping("/suggest-skills")
    public AiResponseDto suggestSkills(@Valid @RequestBody SkillSuggestionRequest request) {
        return aiService.suggestSkills(request, false);
    }

    @PostMapping("/tailor-resume")
    public AiResponseDto tailorResume(@Valid @RequestBody TailorResumeRequest request) {
        return aiService.tailorResumeForJob(request, false);
    }

    @PostMapping("/translate")
    public AiResponseDto translate(@Valid @RequestBody TranslateResumeRequest request) {
        return aiService.translateResume(request, false);
    }

    @PostMapping("/job-fit")
    public JobFitResponse analyzeJobFit(@Valid @RequestBody JobFitRequest request) {
        return aiService.analyzeJobFit(request);
    }

    @GetMapping("/history/{userId}")
    public List<AiHistoryResponse> history(@PathVariable String userId) {
        return aiService.getAiHistory(userId);
    }

    @GetMapping("/quota/{userId}")
    public QuotaResponse quota(@PathVariable String userId,
                               @RequestParam(defaultValue = "false") boolean premium) {
        return aiService.getRemainingQuota(userId, premium);
    }

    @GetMapping("/admin/usage")
    public AiAdminUsageResponse adminUsage() {
        return aiService.getAdminUsageStats();
    }
}
