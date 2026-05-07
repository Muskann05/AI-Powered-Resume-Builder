package com.resumeai.ai.service;

import com.resumeai.ai.client.AuthServiceClient;
import com.resumeai.ai.client.ResumeServiceClient;
import com.resumeai.ai.dto.AuthUserResponse;
import com.resumeai.ai.dto.JobFitRequest;
import com.resumeai.ai.dto.ResumeSummaryResponse;
import com.resumeai.ai.dto.SkillSuggestionRequest;
import com.resumeai.ai.dto.SummaryRequest;
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.enums.AiModel;
import com.resumeai.ai.exception.BadRequestException;
import com.resumeai.ai.repository.AiRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private AiRequestRepository aiRequestRepository;

    @Mock
    private AiProviderService aiProviderService;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @InjectMocks
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        when(authServiceClient.getUserById("user-101")).thenReturn(
                new AuthUserResponse("user-101", "Muskan", "muskan@example.com", "9999999999", "USER", "LOCAL", true, "PREMIUM", null)
        );
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(
                new ResumeSummaryResponse("resume-101", "user-101", "Resume", "Java Developer", "template-101", 80, "DRAFT", "English", false, 0, null, null)
        );
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProviderService.generate(anyString(), anyInt())).thenReturn(
                new AiProviderService.ProviderResult("AI response", AiModel.GEMINI, 120)
        );
    }

    @Test
    void generateSummaryShouldWorkForValidPremiumUser() {
        when(promptBuilder.summaryPrompt("Java Developer", "3", "Java, Spring Boot")).thenReturn("summary prompt");
        when(promptBuilder.sanitize("summary prompt")).thenReturn("summary prompt");

        var response = aiService.generateSummary(
                new SummaryRequest("user-101", "resume-101", "Java Developer", "3", "Java, Spring Boot"),
                false
        );

        assertEquals("AI response", response.aiResponse());
        assertEquals(AiModel.GEMINI, response.model());
    }

    @Test
    void suggestSkillsShouldWorkWithoutResumeId() {
        when(promptBuilder.skillsPrompt("Backend Developer")).thenReturn("skills prompt");
        when(promptBuilder.sanitize("skills prompt")).thenReturn("skills prompt");

        var response = aiService.suggestSkills(new SkillSuggestionRequest("user-101", "Backend Developer"), false);

        assertEquals("AI response", response.aiResponse());
    }

    @Test
    void analyzeJobFitShouldReturnComputedResponse() {
        when(promptBuilder.jobFitPrompt(anyString(), anyString(), anyString())).thenReturn("job fit prompt");
        when(promptBuilder.sanitize("job fit prompt")).thenReturn("job fit prompt");

        var response = aiService.analyzeJobFit(new JobFitRequest(
                "user-101",
                "resume-101",
                "Java Spring Boot MySQL REST APIs",
                "Backend Developer",
                "Need Java Spring Boot Docker MySQL REST APIs"
        ));

        assertEquals("AI response", response.recommendations());
    }

    @Test
    void freeUserShouldBeBlockedForJobFit() {
        when(authServiceClient.getUserById("user-202")).thenReturn(
                new AuthUserResponse("user-202", "Free User", "free@example.com", "9999999999", "USER", "LOCAL", true, "FREE", null)
        );
        when(resumeServiceClient.getResumeById("resume-202")).thenReturn(
                new ResumeSummaryResponse("resume-202", "user-202", "Resume", "Java Developer", "template-101", 80, "DRAFT", "English", false, 0, null, null)
        );

        assertThrows(BadRequestException.class, () -> aiService.analyzeJobFit(new JobFitRequest(
                "user-202",
                "resume-202",
                "Java Spring Boot",
                "Backend Developer",
                "Need Java Spring Boot Docker"
        )));
    }
}
