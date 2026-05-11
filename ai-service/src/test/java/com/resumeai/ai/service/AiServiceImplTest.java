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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private AiRequestRepository aiRequestRepository;

    @Mock
    private AiProviderService aiProviderService;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    private AiServiceImpl aiService;

    private AuthUserResponse freeUser;
    private AuthUserResponse premiumUser;
    private AuthUserResponse inactiveUser;
    private ResumeSummaryResponse resume;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(
                aiRequestRepository,
                aiProviderService,
                new PromptBuilder(),
                authServiceClient,
                resumeServiceClient,
                notificationServiceClient
        );

        freeUser = new AuthUserResponse(
                "user-101",
                "Muskan Gupta",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "FREE",
                null
        );

        premiumUser = new AuthUserResponse(
                "user-101",
                "Muskan Gupta",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "PREMIUM",
                null
        );

        inactiveUser = new AuthUserResponse(
                "user-101",
                "Muskan Gupta",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                false,
                "FREE",
                null
        );

        resume = new ResumeSummaryResponse(
                "resume-101",
                "user-101",
                "Backend Resume",
                "Java Developer",
                "template-101",
                70,
                "DRAFT",
                "English",
                false,
                0,
                null,
                null
        );
    }

    @Test
    void generateSummaryShouldCreateCompletedAiRequestForFreeUserWithinQuota() {
        mockUserAndResume(freeUser);
        mockSuccessfulSaveAndProvider("Generated summary", 120);

        SummaryRequest request = new SummaryRequest(
                "user-101",
                "resume-101",
                "Java Developer",
                "2 years",
                "Java, Spring Boot"
        );

        AiResponseDto response = aiService.generateSummary(request, false);

        assertEquals("user-101", response.userId());
        assertEquals("resume-101", response.resumeId());
        assertEquals(RequestType.SUMMARY, response.requestType());
        assertEquals("Generated summary", response.aiResponse());
        assertEquals(AiModel.GEMINI, response.model());
        assertEquals(120, response.tokensUsed());
        assertEquals(RequestStatus.COMPLETED, response.status());
        assertNotNull(response.requestId());
    }

    @Test
    void generateSummaryShouldRejectInactiveUser() {
        when(authServiceClient.getUserById("user-101")).thenReturn(inactiveUser);

        SummaryRequest request = new SummaryRequest(
                "user-101",
                "resume-101",
                "Java Developer",
                "2 years",
                "Java, Spring Boot"
        );

        assertThrows(BadRequestException.class, () -> aiService.generateSummary(request, false));
        verify(aiRequestRepository, never()).save(any());
    }

    @Test
    void generateSummaryShouldRejectWrongResumeOwner() {
        ResumeSummaryResponse otherUserResume = new ResumeSummaryResponse(
                "resume-101",
                "other-user",
                "Resume",
                "Developer",
                "template-101",
                70,
                "DRAFT",
                "English",
                false,
                0,
                null,
                null
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(otherUserResume);

        SummaryRequest request = new SummaryRequest(
                "user-101",
                "resume-101",
                "Java Developer",
                "2 years",
                "Java, Spring Boot"
        );

        assertThrows(BadRequestException.class, () -> aiService.generateSummary(request, false));
        verify(aiRequestRepository, never()).save(any());
    }

    @Test
    void generateSummaryShouldRejectFreeUserWhenContentQuotaExceeded() {
        mockUserAndResume(freeUser);

        when(aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                eq("user-101"),
                any(RequestType.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0L);

        when(aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                eq("user-101"),
                eq(RequestType.SUMMARY),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(5L);

        SummaryRequest request = new SummaryRequest(
                "user-101",
                "resume-101",
                "Java Developer",
                "2 years",
                "Java, Spring Boot"
        );

        assertThrows(BadRequestException.class, () -> aiService.generateSummary(request, false));
        verify(aiRequestRepository, never()).save(any());
    }

    @Test
    void generateBulletsShouldCreateCompletedAiRequest() {
        mockUserAndResume(freeUser);
        mockSuccessfulSaveAndProvider("Generated bullets", 150);

        BulletPointsRequest request = new BulletPointsRequest(
                "user-101",
                "resume-101",
                "Java Developer",
                "ABC Corp",
                "Built REST APIs"
        );

        AiResponseDto response = aiService.generateBulletPoints(request, false);

        assertEquals(RequestType.BULLETS, response.requestType());
        assertEquals("Generated bullets", response.aiResponse());
        assertEquals(RequestStatus.COMPLETED, response.status());
    }

    @Test
    void generateCoverLetterShouldRejectFreeUser() {
        mockUserAndResume(freeUser);

        CoverLetterRequest request = new CoverLetterRequest(
                "user-101",
                "resume-101",
                "Hiring Java developer",
                "Muskan Gupta"
        );

        assertThrows(BadRequestException.class, () -> aiService.generateCoverLetter(request, false));
        verify(aiRequestRepository, never()).save(any());
    }

    @Test
    void generateCoverLetterShouldWorkForPremiumUser() {
        mockUserAndResume(premiumUser);
        mockSuccessfulSaveAndProvider("Cover letter", 180);

        CoverLetterRequest request = new CoverLetterRequest(
                "user-101",
                "resume-101",
                "Hiring Java developer",
                "Muskan Gupta"
        );

        AiResponseDto response = aiService.generateCoverLetter(request, false);

        assertEquals(RequestType.COVER_LETTER, response.requestType());
        assertEquals("Cover letter", response.aiResponse());
    }

    @Test
    void improveSectionShouldRejectFreeUser() {
        mockUserAndResume(freeUser);

        ImproveSectionRequest request = new ImproveSectionRequest(
                "user-101",
                "resume-101",
                "Summary",
                "Old summary"
        );

        assertThrows(BadRequestException.class, () -> aiService.improveSection(request, false));
    }

    @Test
    void improveSectionShouldWorkForPremiumUser() {
        mockUserAndResume(premiumUser);
        mockSuccessfulSaveAndProvider("Improved section", 100);

        ImproveSectionRequest request = new ImproveSectionRequest(
                "user-101",
                "resume-101",
                "Summary",
                "Old summary"
        );

        AiResponseDto response = aiService.improveSection(request, false);

        assertEquals(RequestType.IMPROVE, response.requestType());
        assertEquals("Improved section", response.aiResponse());
    }

    @Test
    void checkAtsCompatibilityShouldReturnScoreMissingKeywordsAndUpdateResume() {
        mockUserAndResume(freeUser);
        mockSuccessfulSaveAndProvider("Add missing skills", 130);

        AtsCheckRequest request = new AtsCheckRequest(
                "user-101",
                "resume-101",
                "java spring boot rest api mysql",
                "java spring boot microservices aws docker"
        );

        when(resumeServiceClient.updateAtsScore(eq("resume-101"), any(UpdateAtsScoreRequest.class)))
                .thenReturn(resume);

        AtsCheckResponse response = aiService.checkAtsCompatibility(request, false);

        assertEquals("Add missing skills", response.recommendation());
        assertEquals("GEMINI", response.model());
        assertTrue(response.atsScore() > 0);
        assertTrue(response.missingKeywords().contains("microservices"));
        verify(resumeServiceClient).updateAtsScore(eq("resume-101"), any(UpdateAtsScoreRequest.class));
    }

    @Test
    void checkAtsCompatibilityShouldRejectFreeUserWhenAtsQuotaExceeded() {
        mockUserAndResume(freeUser);

        when(aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                eq("user-101"),
                eq(RequestType.ATS),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(3L);

        AtsCheckRequest request = new AtsCheckRequest(
                "user-101",
                "resume-101",
                "java spring boot",
                "java docker"
        );

        assertThrows(BadRequestException.class, () -> aiService.checkAtsCompatibility(request, false));
        verify(aiRequestRepository, never()).save(any());
    }

    @Test
    void suggestSkillsShouldCreateAiRequestWithoutResume() {
        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        mockSuccessfulSaveAndProvider("Java, Spring Boot, SQL", 90);

        SkillSuggestionRequest request = new SkillSuggestionRequest("user-101", "Java Developer");

        AiResponseDto response = aiService.suggestSkills(request, false);

        assertEquals(RequestType.SKILLS, response.requestType());
        assertEquals("Java, Spring Boot, SQL", response.aiResponse());
        assertEquals(null, response.resumeId());
    }

    @Test
    void tailorResumeShouldRejectFreeUser() {
        mockUserAndResume(freeUser);

        TailorResumeRequest request = new TailorResumeRequest(
                "user-101",
                "resume-101",
                "{\"skills\":[\"Java\"]}",
                "Need Spring Boot"
        );

        assertThrows(BadRequestException.class, () -> aiService.tailorResumeForJob(request, false));
    }

    @Test
    void tailorResumeShouldWorkForPremiumUser() {
        mockUserAndResume(premiumUser);
        mockSuccessfulSaveAndProvider("{\"skills\":[\"Java\",\"Spring Boot\"]}", 200);

        TailorResumeRequest request = new TailorResumeRequest(
                "user-101",
                "resume-101",
                "{\"skills\":[\"Java\"]}",
                "Need Spring Boot"
        );

        AiResponseDto response = aiService.tailorResumeForJob(request, false);

        assertEquals(RequestType.TAILOR, response.requestType());
        assertTrue(response.aiResponse().contains("Spring Boot"));
    }

    @Test
    void translateResumeShouldRejectFreeUser() {
        mockUserAndResume(freeUser);

        TranslateResumeRequest request = new TranslateResumeRequest(
                "user-101",
                "resume-101",
                "Java developer",
                "Hindi"
        );

        assertThrows(BadRequestException.class, () -> aiService.translateResume(request, false));
    }

    @Test
    void translateResumeShouldWorkForPremiumUser() {
        mockUserAndResume(premiumUser);
        mockSuccessfulSaveAndProvider("Translated resume", 110);

        TranslateResumeRequest request = new TranslateResumeRequest(
                "user-101",
                "resume-101",
                "Java developer",
                "Hindi"
        );

        AiResponseDto response = aiService.translateResume(request, false);

        assertEquals(RequestType.TRANSLATE, response.requestType());
        assertEquals("Translated resume", response.aiResponse());
    }

    @Test
    void analyzeJobFitShouldRejectFreeUser() {
        mockUserAndResume(freeUser);

        JobFitRequest request = new JobFitRequest(
                "user-101",
                "resume-101",
                "java spring boot",
                "Backend Developer",
                "java aws docker"
        );

        assertThrows(BadRequestException.class, () -> aiService.analyzeJobFit(request));
    }

    @Test
    void analyzeJobFitShouldWorkForPremiumUser() {
        mockUserAndResume(premiumUser);
        mockSuccessfulSaveAndProvider("Tailor resume for AWS and Docker", 170);

        JobFitRequest request = new JobFitRequest(
                "user-101",
                "resume-101",
                "java spring boot",
                "Backend Developer",
                "java aws docker"
        );

        JobFitResponse response = aiService.analyzeJobFit(request);

        assertTrue(response.matchScore() > 0);
        assertTrue(response.missingSkills().contains("aws"));
        assertEquals("Tailor resume for AWS and Docker", response.recommendations());
    }

    @Test
    void getAiHistoryShouldReturnUserHistory() {
        AiRequest request = aiRequest(
                "request-101",
                "user-101",
                "resume-101",
                RequestType.SUMMARY,
                RequestStatus.COMPLETED,
                AiModel.GEMINI,
                100,
                "Summary response"
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc("user-101")).thenReturn(List.of(request));

        List<AiHistoryResponse> history = aiService.getAiHistory("user-101");

        assertEquals(1, history.size());
        assertEquals("request-101", history.get(0).requestId());
        assertEquals(RequestType.SUMMARY, history.get(0).requestType());
    }

    @Test
    void getRemainingQuotaShouldReturnUnlimitedForPremiumUser() {
        when(authServiceClient.getUserById("user-101")).thenReturn(premiumUser);

        QuotaResponse response = aiService.getRemainingQuota("user-101", false);

        assertEquals(Long.MAX_VALUE, response.remainingContentCalls());
        assertEquals(Long.MAX_VALUE, response.remainingAtsChecks());
        assertTrue(response.premium());
    }

    @Test
    void getRemainingQuotaShouldReturnRemainingForFreeUser() {
        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);

        when(aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                eq("user-101"),
                any(RequestType.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0L);

        when(aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                eq("user-101"),
                eq(RequestType.SUMMARY),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(2L);

        when(aiRequestRepository.countByUserIdAndRequestTypeAndCreatedAtBetween(
                eq("user-101"),
                eq(RequestType.ATS),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(1L);

        QuotaResponse response = aiService.getRemainingQuota("user-101", false);

        assertEquals(3, response.remainingContentCalls());
        assertEquals(2, response.remainingAtsChecks());
        assertFalse(response.premium());
    }

    @Test
    void saveAndGenerateShouldMarkFailedWhenProviderThrows() {
        mockUserAndResume(freeUser);
        mockRepositorySave();

        when(aiProviderService.generate(anyString(), anyInt()))
                .thenThrow(new RuntimeException("provider unavailable"));

        SummaryRequest request = new SummaryRequest(
                "user-101",
                "resume-101",
                "Java Developer",
                "2 years",
                "Java, Spring Boot"
        );

        AiResponseDto response = aiService.generateSummary(request, false);

        assertEquals(RequestStatus.FAILED, response.status());
        assertTrue(response.aiResponse().contains("AI generation failed"));
    }

    @Test
    void getAdminUsageStatsShouldAggregateRequests() {
        AiRequest completedGemini = aiRequest(
                "request-101",
                "user-101",
                "resume-101",
                RequestType.SUMMARY,
                RequestStatus.COMPLETED,
                AiModel.GEMINI,
                100,
                "Summary"
        );

        AiRequest failedFallback = aiRequest(
                "request-102",
                "user-102",
                "resume-202",
                RequestType.ATS,
                RequestStatus.FAILED,
                AiModel.FALLBACK,
                20,
                "Failed"
        );

        when(aiRequestRepository.findAll()).thenReturn(List.of(completedGemini, failedFallback));

        AiAdminUsageResponse response = aiService.getAdminUsageStats();

        assertEquals(2, response.totalRequests());
        assertEquals(1, response.completedRequests());
        assertEquals(1, response.failedRequests());
        assertEquals(120, response.totalTokensUsed());
        assertEquals(2, response.byModel().size());
        assertEquals(2, response.byUser().size());
    }

    private void mockUserAndResume(AuthUserResponse user) {
        when(authServiceClient.getUserById("user-101")).thenReturn(user);
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
    }

    private void mockSuccessfulSaveAndProvider(String output, int tokensUsed) {
        mockRepositorySave();
        when(aiProviderService.generate(anyString(), eq(1000)))
                .thenReturn(new AiProviderService.ProviderResult(output, AiModel.GEMINI, tokensUsed));
    }

    private void mockRepositorySave() {
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> {
            AiRequest request = invocation.getArgument(0);
            if (request.getRequestId() == null) {
                request.prePersist();
            }
            return request;
        });
    }

    private AiRequest aiRequest(String requestId,
                                String userId,
                                String resumeId,
                                RequestType requestType,
                                RequestStatus status,
                                AiModel model,
                                Integer tokensUsed,
                                String aiResponse) {
        AiRequest request = new AiRequest();
        request.setRequestId(requestId);
        request.setUserId(userId);
        request.setResumeId(resumeId);
        request.setRequestType(requestType);
        request.setInputPrompt("Prompt");
        request.setStatus(status);
        request.setModel(model);
        request.setTokensUsed(tokensUsed);
        request.setAiResponse(aiResponse);
        request.prePersist();
        request.setCompletedAt(LocalDateTime.now());
        return request;
    }
}
