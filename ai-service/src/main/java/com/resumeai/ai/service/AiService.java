package com.resumeai.ai.service;

import com.resumeai.ai.dto.*;

import java.util.List;

public interface AiService {

    AiResponseDto generateSummary(SummaryRequest request, boolean premium);

    AiResponseDto generateBulletPoints(BulletPointsRequest request, boolean premium);

    AiResponseDto generateCoverLetter(CoverLetterRequest request, boolean premium);

    AiResponseDto improveSection(ImproveSectionRequest request, boolean premium);

    AtsCheckResponse checkAtsCompatibility(AtsCheckRequest request, boolean premium);

    AiResponseDto suggestSkills(SkillSuggestionRequest request, boolean premium);

    AiResponseDto tailorResumeForJob(TailorResumeRequest request, boolean premium);

    AiResponseDto translateResume(TranslateResumeRequest request, boolean premium);

    JobFitResponse analyzeJobFit(JobFitRequest request);

    List<AiHistoryResponse> getAiHistory(String userId);

    QuotaResponse getRemainingQuota(String userId, boolean premium);

    AiAdminUsageResponse getAdminUsageStats();
}
