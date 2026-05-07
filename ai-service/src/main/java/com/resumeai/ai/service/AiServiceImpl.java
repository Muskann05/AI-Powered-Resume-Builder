package com.resumeai.ai.service;

import com.resumeai.ai.client.AuthServiceClient;
import com.resumeai.ai.client.NotificationServiceClient;
import com.resumeai.ai.client.ResumeServiceClient;
import com.resumeai.ai.dto.*;
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.enums.AiModel;
import com.resumeai.ai.enums.RequestStatus;
import com.resumeai.ai.enums.RequestType;
import com.resumeai.ai.exception.BadRequestException;
import com.resumeai.ai.repository.AiRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiRequestRepository aiRequestRepository;
    private final AiProviderService aiProviderService;
    private final PromptBuilder promptBuilder;
    private final AuthServiceClient authServiceClient;
    private final ResumeServiceClient resumeServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public AiServiceImpl(AiRequestRepository aiRequestRepository,
                         AiProviderService aiProviderService,
                         PromptBuilder promptBuilder,
                         AuthServiceClient authServiceClient,
                         ResumeServiceClient resumeServiceClient,
                         NotificationServiceClient notificationServiceClient) {
        this.aiRequestRepository = aiRequestRepository;
        this.aiProviderService = aiProviderService;
        this.promptBuilder = promptBuilder;
        this.authServiceClient = authServiceClient;
        this.resumeServiceClient = resumeServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    @Override
    public AiResponseDto generateSummary(SummaryRequest request, boolean premium) {
        log.info("Generating summary userId={} resumeId={}", request.userId(), request.resumeId());
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        enforceContentQuota(request.userId(), resolvedPremium);
        AiResponseDto response = saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.SUMMARY,
                promptBuilder.summaryPrompt(request.jobTitle(), request.yearsOfExperience(), request.keySkills())
        );
        notifyQuotaNearLimitIfNeeded(request.userId(), request.resumeId(), resolvedPremium);
        return response;
    }

    @Override
    public AiResponseDto generateBulletPoints(BulletPointsRequest request, boolean premium) {
        log.info("Generating bullet points userId={} resumeId={}", request.userId(), request.resumeId());
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        enforceContentQuota(request.userId(), resolvedPremium);
        AiResponseDto response = saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.BULLETS,
                promptBuilder.bulletPrompt(request.jobRole(), request.companyName(), request.responsibilities())
        );
        notifyQuotaNearLimitIfNeeded(request.userId(), request.resumeId(), resolvedPremium);
        return response;
    }

    @Override
    public AiResponseDto generateCoverLetter(CoverLetterRequest request, boolean premium) {
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        if (!resolvedPremium) {
            throw new BadRequestException("Cover letter generation is premium only");
        }
        return saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.COVER_LETTER,
                promptBuilder.coverLetterPrompt(request.applicantName(), request.jobDescription())
        );
    }

    @Override
    public AiResponseDto improveSection(ImproveSectionRequest request, boolean premium) {
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        if (!resolvedPremium) {
            throw new BadRequestException("Improve section is premium only");
        }
        return saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.IMPROVE,
                promptBuilder.improveSectionPrompt(request.sectionName(), request.currentContent())
        );
    }

    @Override
    public AtsCheckResponse checkAtsCompatibility(AtsCheckRequest request, boolean premium) {
        log.info("Running ATS check userId={} resumeId={}", request.userId(), request.resumeId());
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        enforceAtsQuota(request.userId(), resolvedPremium);

        List<String> resumeWords = tokenize(request.resumeText());
        List<String> jdWords = tokenize(request.jobDescription());

        List<String> missingKeywords = jdWords.stream()
                .distinct()
                .filter(word -> !resumeWords.contains(word))
                .limit(12)
                .toList();

        int matched = (int) jdWords.stream().distinct().filter(resumeWords::contains).count();
        int total = Math.max(1, (int) jdWords.stream().distinct().count());
        int atsScore = Math.min(100, (matched * 100) / total);

        AiResponseDto recommendation = saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.ATS,
                promptBuilder.atsRecommendationPrompt(
                        request.resumeText(),
                        request.jobDescription(),
                        atsScore,
                        String.join(", ", missingKeywords)
                )
        );

        resumeServiceClient.updateAtsScore(request.resumeId(), new UpdateAtsScoreRequest(atsScore));
        notifyQuotaNearLimitIfNeeded(request.userId(), request.resumeId(), resolvedPremium);

        return new AtsCheckResponse(
                recommendation.requestId(),
                atsScore,
                missingKeywords,
                recommendation.aiResponse(),
                recommendation.model() != null ? recommendation.model().name() : "UNKNOWN"
        );
    }

    @Override
    public AiResponseDto suggestSkills(SkillSuggestionRequest request, boolean premium) {
        log.info("Generating skill suggestions userId={}", request.userId());
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        enforceContentQuota(request.userId(), resolvedPremium);
        AiResponseDto response = saveAndGenerate(
                request.userId(),
                null,
                RequestType.SKILLS,
                promptBuilder.skillsPrompt(request.targetJobTitle())
        );
        notifyQuotaNearLimitIfNeeded(request.userId(), null, resolvedPremium);
        return response;
    }

    @Override
    public AiResponseDto tailorResumeForJob(TailorResumeRequest request, boolean premium) {
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        if (!resolvedPremium) {
            throw new BadRequestException("Tailor resume is premium only");
        }
        return saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.TAILOR,
                promptBuilder.tailorPrompt(request.resumeJson(), request.jobDescription())
        );
    }

    @Override
    public AiResponseDto translateResume(TranslateResumeRequest request, boolean premium) {
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        if (!resolvedPremium) {
            throw new BadRequestException("Resume translation is premium only");
        }
        return saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.TRANSLATE,
                promptBuilder.translatePrompt(request.resumeText(), request.targetLanguage())
        );
    }

    @Override
    public JobFitResponse analyzeJobFit(JobFitRequest request) {
        boolean resolvedPremium = validateUserAndResolvePremium(request.userId());
        validateResumeOwnership(request.userId(), request.resumeId());
        if (!resolvedPremium) {
            throw new BadRequestException("Job fit analysis is premium only");
        }

        List<String> resumeWords = tokenize(request.resumeContent());
        List<String> jobWords = tokenize(request.jobDescription());

        List<String> missingSkills = jobWords.stream()
                .distinct()
                .filter(word -> !resumeWords.contains(word))
                .limit(10)
                .toList();

        int matched = (int) jobWords.stream().distinct().filter(resumeWords::contains).count();
        int total = Math.max(1, (int) jobWords.stream().distinct().count());
        int score = Math.min(100, (matched * 100) / total);

        AiResponseDto recommendation = saveAndGenerate(
                request.userId(),
                request.resumeId(),
                RequestType.JOB_FIT,
                promptBuilder.jobFitPrompt(request.resumeContent(), request.jobTitle(), request.jobDescription())
        );

        return new JobFitResponse(score, missingSkills, recommendation.aiResponse());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHistoryResponse> getAiHistory(String userId) {
        validateUserAndResolvePremium(userId);
        return aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(req -> new AiHistoryResponse(
                        req.getRequestId(),
                        req.getResumeId(),
                        req.getRequestType(),
                        req.getModel(),
                        req.getTokensUsed(),
                        req.getStatus(),
                        req.getCreatedAt(),
                        req.getCompletedAt(),
                        req.getAiResponse()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaResponse getRemainingQuota(String userId, boolean premium) {
        boolean resolvedPremium = validateUserAndResolvePremium(userId);
        if (resolvedPremium) {
            return new QuotaResponse(Long.MAX_VALUE, Long.MAX_VALUE, true);
        }

        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        long contentUsed = countContentCalls(userId, start, end);
        long atsUsed = aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                userId, RequestType.ATS, start, end
        );

        return new QuotaResponse(
                Math.max(0, 5 - contentUsed),
                Math.max(0, 3 - atsUsed),
                false
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AiAdminUsageResponse getAdminUsageStats() {
        List<AiRequest> requests = aiRequestRepository.findAll();

        long completed = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.COMPLETED)
                .count();

        long failed = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.FAILED)
                .count();

        long tokens = requests.stream()
                .mapToLong(request -> request.getTokensUsed() != null ? request.getTokensUsed() : 0)
                .sum();

        Map<AiModel, List<AiRequest>> byModelMap = requests.stream()
                .filter(request -> request.getModel() != null)
                .collect(Collectors.groupingBy(AiRequest::getModel));

        List<AiUsageByModelResponse> byModel = byModelMap.entrySet().stream()
                .map(entry -> new AiUsageByModelResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .mapToLong(request -> request.getTokensUsed() != null ? request.getTokensUsed() : 0)
                                .sum()
                ))
                .toList();

        Map<String, List<AiRequest>> byUserMap = requests.stream()
                .collect(Collectors.groupingBy(AiRequest::getUserId));

        List<AiUsageByUserResponse> byUser = byUserMap.entrySet().stream()
                .map(entry -> new AiUsageByUserResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .mapToLong(request -> request.getTokensUsed() != null ? request.getTokensUsed() : 0)
                                .sum()
                ))
                .toList();

        return new AiAdminUsageResponse(
                requests.size(),
                completed,
                failed,
                tokens,
                byModel,
                byUser
        );
    }

    private boolean validateUserAndResolvePremium(String userId) {
        AuthUserResponse user = authServiceClient.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BadRequestException("User account is inactive");
        }
        return "PREMIUM".equalsIgnoreCase(user.subscriptionPlan());
    }

    private void validateResumeOwnership(String userId, String resumeId) {
        if (resumeId == null || resumeId.isBlank()) {
            return;
        }
        ResumeSummaryResponse resume = resumeServiceClient.getResumeById(resumeId);
        if (resume == null || resume.userId() == null || !resume.userId().equals(userId)) {
            throw new BadRequestException("Resume does not belong to the user");
        }
    }

    private AiResponseDto saveAndGenerate(String userId, String resumeId, RequestType requestType, String prompt) {
        log.debug("Saving AI request userId={} resumeId={} type={}", userId, resumeId, requestType);
        AiRequest entity = new AiRequest();
        entity.setUserId(userId);
        entity.setResumeId(resumeId);
        entity.setRequestType(requestType);
        entity.setInputPrompt(promptBuilder.sanitize(prompt));
        entity.setStatus(RequestStatus.QUEUED);
        entity = aiRequestRepository.save(entity);

        try {
            AiProviderService.ProviderResult result = aiProviderService.generate(entity.getInputPrompt(), 1000);
            entity.setAiResponse(result.output());
            entity.setModel(result.model());
            entity.setTokensUsed(result.tokensUsed());
            entity.setStatus(RequestStatus.COMPLETED);
            log.info("AI request completed requestId={} type={} model={} tokens={}",
                    entity.getRequestId(), requestType, result.model(), result.tokensUsed());
        } catch (Exception ex) {
            entity.setAiResponse("AI generation failed: " + ex.getMessage());
            entity.setStatus(RequestStatus.FAILED);
            log.warn("AI request failed requestId={} type={} reason={}",
                    entity.getRequestId(), requestType, ex.getMessage());
        }

        entity.setCompletedAt(LocalDateTime.now());
        entity = aiRequestRepository.save(entity);

        return new AiResponseDto(
                entity.getRequestId(),
                entity.getUserId(),
                entity.getResumeId(),
                entity.getRequestType(),
                entity.getAiResponse(),
                entity.getModel(),
                entity.getTokensUsed(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    private void enforceContentQuota(String userId, boolean premium) {
        if (premium) {
            return;
        }

        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        if (countContentCalls(userId, start, end) >= 5) {
            throw new BadRequestException("Monthly AI content quota exceeded for free tier");
        }
    }

    private void enforceAtsQuota(String userId, boolean premium) {
        if (premium) {
            return;
        }

        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        long atsUsed = aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                userId, RequestType.ATS, start, end
        );

        if (atsUsed >= 3) {
            throw new BadRequestException("Monthly ATS quota exceeded for free tier");
        }
    }

    private long countContentCalls(String userId, LocalDateTime start, LocalDateTime end) {
        return aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.SUMMARY, start, end)
                + aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.BULLETS, start, end)
                + aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.COVER_LETTER, start, end)
                + aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.IMPROVE, start, end)
                + aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.SKILLS, start, end)
                + aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.TAILOR, start, end)
                + aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(userId, RequestType.TRANSLATE, start, end);
    }

    private List<String> tokenize(String input) {
        return Arrays.stream(input.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(s -> s.length() > 2)
                .toList();
    }

    private void notifyQuotaNearLimitIfNeeded(String userId, String resumeId, boolean premium) {
        if (premium) {
            return;
        }

        QuotaResponse quota = getRemainingQuota(userId, false);
        boolean contentNearLimit = quota.remainingContentCalls() <= 1;
        boolean atsNearLimit = quota.remainingAtsChecks() <= 1;
        if (!contentNearLimit && !atsNearLimit) {
            return;
        }

        String message = "You are close to your monthly free AI quota. Remaining content calls: "
                + quota.remainingContentCalls() + ", remaining ATS checks: " + quota.remainingAtsChecks() + ".";

        try {
            notificationServiceClient.sendNotification(new NotificationRequest(
                    userId,
                    "QUOTA_NEARING_LIMIT",
                    "AI quota nearing limit",
                    message,
                    "APP",
                    resumeId,
                    "RESUME",
                    "/ai/quota/" + userId
            ));
            log.info("Sent quota nearing limit notification userId={}", userId);
        } catch (Exception ex) {
            log.warn("Failed to send quota nearing limit notification userId={} reason={}",
                    userId, ex.getMessage());
        }
    }
}
