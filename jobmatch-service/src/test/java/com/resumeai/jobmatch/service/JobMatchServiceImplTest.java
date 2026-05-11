package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.client.AiServiceClient;
import com.resumeai.jobmatch.client.AuthServiceClient;
import com.resumeai.jobmatch.client.LinkedInJobClient;
import com.resumeai.jobmatch.client.NotificationServiceClient;
import com.resumeai.jobmatch.client.ResumeServiceClient;
import com.resumeai.jobmatch.client.SectionServiceClient;
import com.resumeai.jobmatch.dto.AiJobFitResponse;
import com.resumeai.jobmatch.dto.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.JobMatchResponse;
import com.resumeai.jobmatch.dto.JobSearchRequest;
import com.resumeai.jobmatch.dto.LinkedInJobResponse;
import com.resumeai.jobmatch.dto.ResumeResponse;
import com.resumeai.jobmatch.dto.SectionResponse;
import com.resumeai.jobmatch.dto.TailoringRecommendationsResponse;
import com.resumeai.jobmatch.dto.UserResponse;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.enums.JobSource;
import com.resumeai.jobmatch.exception.BadRequestException;
import com.resumeai.jobmatch.exception.ResourceNotFoundException;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceImplTest {

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @Mock
    private SectionServiceClient sectionServiceClient;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private LinkedInJobClient linkedInJobClient;

    @Mock
    private LinkedInJobFallbackService linkedInJobFallbackService;

    private JobMatchServiceImpl jobMatchService;

    private UserResponse premiumUser;
    private UserResponse freeUser;
    private UserResponse inactiveUser;
    private ResumeResponse resume;
    private List<SectionResponse> sections;

    @BeforeEach
    void setUp() {
        jobMatchService = new JobMatchServiceImpl(
                jobMatchRepository,
                authServiceClient,
                resumeServiceClient,
                sectionServiceClient,
                aiServiceClient,
                notificationServiceClient,
                linkedInJobClient,
                linkedInJobFallbackService
        );

        ReflectionTestUtils.setField(jobMatchService, "linkedinApiKey", "test-linkedin-key");
        ReflectionTestUtils.setField(jobMatchService, "linkedinDefaultLimit", 5);
        ReflectionTestUtils.setField(jobMatchService, "linkedinApiProvider", "dummy");
        ReflectionTestUtils.setField(jobMatchService, "linkedinFallbackEnabled", true);
        ReflectionTestUtils.setField(jobMatchService, "notificationThreshold", 80);

        premiumUser = new UserResponse(
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

        freeUser = new UserResponse(
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

        inactiveUser = new UserResponse(
                "user-101",
                "Muskan Gupta",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                false,
                "PREMIUM",
                null
        );

        resume = new ResumeResponse(
                "resume-101",
                "user-101",
                "Backend Resume",
                "Java Developer",
                "template-101",
                82,
                "DRAFT",
                "English",
                false,
                0,
                null,
                null
        );

        sections = List.of(
                new SectionResponse(
                        "section-101",
                        "resume-101",
                        "SUMMARY",
                        "Summary",
                        "Java backend developer",
                        1,
                        true,
                        false,
                        null,
                        null
                )
        );
    }

    @Test
    void analyzeJobFitShouldCreateManualMatchForPremiumUser() {
        mockPremiumUserResumeAndSections();
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(88, "AWS,Docker", "Add cloud projects"));
        mockSave();

        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest(
                "resume-101",
                "user-101",
                "Backend Developer",
                "Need Java, Spring Boot, AWS and Docker"
        );

        JobMatchResponse response = jobMatchService.analyzeJobFit(request);

        assertNotNull(response.matchId());
        assertEquals("resume-101", response.resumeId());
        assertEquals("user-101", response.userId());
        assertEquals("Backend Developer", response.jobTitle());
        assertEquals(88, response.matchScore());
        assertEquals("AWS,Docker", response.missingSkills());
        assertEquals("Add cloud projects", response.recommendations());
        assertEquals(JobSource.MANUAL, response.source());
        assertFalse(response.isBookmarked());

        verify(notificationServiceClient).sendNotification(any());
        verify(jobMatchRepository).save(any(JobMatch.class));
    }

    @Test
    void analyzeJobFitShouldSkipNotificationWhenScoreBelowThreshold() {
        mockPremiumUserResumeAndSections();
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(70, "AWS", "Improve skills"));
        mockSave();

        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest(
                "resume-101",
                "user-101",
                "Backend Developer",
                "Need Java and AWS"
        );

        JobMatchResponse response = jobMatchService.analyzeJobFit(request);

        assertEquals(70, response.matchScore());
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    void analyzeJobFitShouldUseFallbackScoreWhenAiResponseMissingScore() {
        mockPremiumUserResumeAndSections();
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(null, "AWS", "Improve skills"));
        mockSave();

        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest(
                "resume-101",
                "user-101",
                "Backend Developer",
                "Need Java and AWS"
        );

        JobMatchResponse response = jobMatchService.analyzeJobFit(request);

        assertEquals(60, response.matchScore());
    }

    @Test
    void analyzeJobFitShouldRejectFreeUser() {
        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);

        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest(
                "resume-101",
                "user-101",
                "Backend Developer",
                "Need Java"
        );

        assertThrows(BadRequestException.class, () -> jobMatchService.analyzeJobFit(request));
        verify(jobMatchRepository, never()).save(any());
    }

    @Test
    void analyzeJobFitShouldRejectInactiveUser() {
        when(authServiceClient.getUserById("user-101")).thenReturn(inactiveUser);

        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest(
                "resume-101",
                "user-101",
                "Backend Developer",
                "Need Java"
        );

        assertThrows(BadRequestException.class, () -> jobMatchService.analyzeJobFit(request));
        verify(jobMatchRepository, never()).save(any());
    }

    @Test
    void analyzeJobFitShouldRejectWrongResumeOwner() {
        ResumeResponse otherUserResume = new ResumeResponse(
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

        when(authServiceClient.getUserById("user-101")).thenReturn(premiumUser);
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(otherUserResume);

        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest(
                "resume-101",
                "user-101",
                "Backend Developer",
                "Need Java"
        );

        assertThrows(BadRequestException.class, () -> jobMatchService.analyzeJobFit(request));
        verify(jobMatchRepository, never()).save(any());
    }

    @Test
    void fetchLinkedInJobsShouldUseDummyProviderAndSaveMatches() {
        mockPremiumUserResumeAndSections();

        List<LinkedInJobResponse> linkedInJobs = List.of(
                new LinkedInJobResponse(
                        "linkedin-101",
                        "Java Developer",
                        "TechNova",
                        "Remote",
                        "Need Java and Spring Boot",
                        "https://linkedin.com/jobs/101"
                ),
                new LinkedInJobResponse(
                        "linkedin-102",
                        "Backend Engineer",
                        "HireFlow",
                        "Bengaluru",
                        "Need APIs and SQL",
                        "https://linkedin.com/jobs/102"
                )
        );

        when(linkedInJobFallbackService.generateJobs("Java Developer", "Remote", 2))
                .thenReturn(linkedInJobs);
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(85, "AWS", "Add AWS"))
                .thenReturn(new AiJobFitResponse(78, "SQL", "Add SQL"));
        mockSave();

        JobSearchRequest request = new JobSearchRequest(
                "resume-101",
                "user-101",
                "Java Developer",
                "Remote",
                2
        );

        List<JobMatchResponse> responses = jobMatchService.fetchLinkedInJobs(request);

        assertEquals(2, responses.size());
        assertEquals(JobSource.LINKEDIN, responses.get(0).source());
        assertEquals("linkedin-101", responses.get(0).externalJobId());
        assertEquals("TechNova", responses.get(0).companyName());
        assertEquals(85, responses.get(0).matchScore());

        verify(linkedInJobFallbackService).generateJobs("Java Developer", "Remote", 2);
        verify(linkedInJobClient, never()).searchJobs(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void fetchLinkedInJobsShouldUseDefaultLimitWhenLimitIsNull() {
        mockPremiumUserResumeAndSections();

        List<LinkedInJobResponse> linkedInJobs = List.of(
                new LinkedInJobResponse(
                        "linkedin-101",
                        "Java Developer",
                        "TechNova",
                        "Remote",
                        "Need Java",
                        "https://linkedin.com/jobs/101"
                )
        );

        when(linkedInJobFallbackService.generateJobs("Java Developer", "Remote", 5))
                .thenReturn(linkedInJobs);
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(82, "AWS", "Add AWS"));
        mockSave();

        JobSearchRequest request = new JobSearchRequest(
                "resume-101",
                "user-101",
                "Java Developer",
                "Remote",
                null
        );

        List<JobMatchResponse> responses = jobMatchService.fetchLinkedInJobs(request);

        assertEquals(1, responses.size());
        verify(linkedInJobFallbackService).generateJobs("Java Developer", "Remote", 5);
    }

    @Test
    void fetchLinkedInJobsShouldUseRemoteProviderWhenConfigured() {
        ReflectionTestUtils.setField(jobMatchService, "linkedinApiProvider", "rapidapi");

        mockPremiumUserResumeAndSections();

        List<LinkedInJobResponse> linkedInJobs = List.of(
                new LinkedInJobResponse(
                        "linkedin-201",
                        "Java Developer",
                        "RemoteCo",
                        "Remote",
                        "Need Java",
                        "https://linkedin.com/jobs/201"
                )
        );

        when(linkedInJobClient.searchJobs("Java Developer", "Remote", 1, "test-linkedin-key"))
                .thenReturn(linkedInJobs);
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(90, "Docker", "Add Docker"));
        mockSave();

        JobSearchRequest request = new JobSearchRequest(
                "resume-101",
                "user-101",
                "Java Developer",
                "Remote",
                1
        );

        List<JobMatchResponse> responses = jobMatchService.fetchLinkedInJobs(request);

        assertEquals(1, responses.size());
        assertEquals("RemoteCo", responses.get(0).companyName());
        verify(linkedInJobClient).searchJobs("Java Developer", "Remote", 1, "test-linkedin-key");
    }

    @Test
    void fetchLinkedInJobsShouldFallbackWhenRemoteProviderFails() {
        ReflectionTestUtils.setField(jobMatchService, "linkedinApiProvider", "rapidapi");

        mockPremiumUserResumeAndSections();

        List<LinkedInJobResponse> fallbackJobs = List.of(
                new LinkedInJobResponse(
                        "fallback-101",
                        "Java Developer",
                        "FallbackCo",
                        "Remote",
                        "Need Java",
                        "https://linkedin.com/jobs/fallback-101"
                )
        );

        when(linkedInJobClient.searchJobs("Java Developer", "Remote", 1, "test-linkedin-key"))
                .thenThrow(new RuntimeException("remote down"));
        when(linkedInJobFallbackService.generateJobs("Java Developer", "Remote", 1))
                .thenReturn(fallbackJobs);
        when(aiServiceClient.analyzeJobFit(any()))
                .thenReturn(new AiJobFitResponse(83, "Docker", "Add Docker"));
        mockSave();

        JobSearchRequest request = new JobSearchRequest(
                "resume-101",
                "user-101",
                "Java Developer",
                "Remote",
                1
        );

        List<JobMatchResponse> responses = jobMatchService.fetchLinkedInJobs(request);

        assertEquals(1, responses.size());
        assertEquals("FallbackCo", responses.get(0).companyName());
        verify(linkedInJobFallbackService).generateJobs("Java Developer", "Remote", 1);
    }

    @Test
    void fetchLinkedInJobsShouldThrowWhenRemoteFailsAndFallbackDisabled() {
        ReflectionTestUtils.setField(jobMatchService, "linkedinApiProvider", "rapidapi");
        ReflectionTestUtils.setField(jobMatchService, "linkedinFallbackEnabled", false);

        mockPremiumUserResumeAndSections();

        when(linkedInJobClient.searchJobs("Java Developer", "Remote", 1, "test-linkedin-key"))
                .thenThrow(new RuntimeException("remote down"));

        JobSearchRequest request = new JobSearchRequest(
                "resume-101",
                "user-101",
                "Java Developer",
                "Remote",
                1
        );

        assertThrows(BadRequestException.class, () -> jobMatchService.fetchLinkedInJobs(request));
        verify(jobMatchRepository, never()).save(any());
    }

    @Test
    void getMatchesByResumeShouldReturnMatches() {
        JobMatch match = jobMatch("match-101", 88, JobSource.MANUAL);

        when(jobMatchRepository.findByResumeIdOrderByMatchedAtDesc("resume-101"))
                .thenReturn(List.of(match));

        List<JobMatchResponse> responses = jobMatchService.getMatchesByResume("resume-101");

        assertEquals(1, responses.size());
        assertEquals("match-101", responses.get(0).matchId());
    }

    @Test
    void getMatchesByUserShouldReturnMatches() {
        JobMatch match = jobMatch("match-101", 88, JobSource.MANUAL);

        when(jobMatchRepository.findByUserIdOrderByMatchedAtDesc("user-101"))
                .thenReturn(List.of(match));

        List<JobMatchResponse> responses = jobMatchService.getMatchesByUser("user-101");

        assertEquals(1, responses.size());
        assertEquals("user-101", responses.get(0).userId());
    }

    @Test
    void getTopMatchesShouldReturnTopMatches() {
        JobMatch match = jobMatch("match-101", 95, JobSource.LINKEDIN);

        when(jobMatchRepository.findTop10ByUserIdOrderByMatchScoreDescMatchedAtDesc("user-101"))
                .thenReturn(List.of(match));

        List<JobMatchResponse> responses = jobMatchService.getTopMatches("user-101");

        assertEquals(1, responses.size());
        assertEquals(95, responses.get(0).matchScore());
    }

    @Test
    void getMatchByIdShouldReturnMatch() {
        JobMatch match = jobMatch("match-101", 88, JobSource.MANUAL);

        when(jobMatchRepository.findByMatchId("match-101")).thenReturn(Optional.of(match));

        JobMatchResponse response = jobMatchService.getMatchById("match-101");

        assertEquals("match-101", response.matchId());
        assertEquals(88, response.matchScore());
    }

    @Test
    void getMatchByIdShouldThrowWhenMissing() {
        when(jobMatchRepository.findByMatchId("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobMatchService.getMatchById("missing"));
    }

    @Test
    void bookmarkMatchShouldUpdateBookmark() {
        JobMatch match = jobMatch("match-101", 88, JobSource.MANUAL);
        match.setIsBookmarked(false);

        when(jobMatchRepository.findByMatchId("match-101")).thenReturn(Optional.of(match));
        when(jobMatchRepository.save(match)).thenReturn(match);

        JobMatchResponse response = jobMatchService.bookmarkMatch("match-101", true);

        assertTrue(response.isBookmarked());
        verify(jobMatchRepository).save(match);
    }

    @Test
    void getRecommendationsShouldReturnRecommendations() {
        JobMatch match = jobMatch("match-101", 88, JobSource.MANUAL);
        match.setRecommendations("Add AWS projects");

        when(jobMatchRepository.findByMatchId("match-101")).thenReturn(Optional.of(match));

        TailoringRecommendationsResponse response = jobMatchService.getRecommendations("match-101");

        assertEquals("match-101", response.matchId());
        assertEquals("Add AWS projects", response.recommendations());
    }

    @Test
    void deleteMatchShouldDeleteMatch() {
        JobMatch match = jobMatch("match-101", 88, JobSource.MANUAL);

        when(jobMatchRepository.findByMatchId("match-101")).thenReturn(Optional.of(match));

        jobMatchService.deleteMatch("match-101");

        verify(jobMatchRepository).delete(match);
    }

    private void mockPremiumUserResumeAndSections() {
        when(authServiceClient.getUserById("user-101")).thenReturn(premiumUser);
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionServiceClient.getSectionsByResume("resume-101")).thenReturn(sections);
    }

    private void mockSave() {
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(invocation -> {
            JobMatch match = invocation.getArgument(0);
            if (match.getMatchId() == null) {
                match.prePersist();
            }
            return match;
        });
    }

    private JobMatch jobMatch(String matchId, Integer score, JobSource source) {
        JobMatch match = new JobMatch();
        match.setMatchId(matchId);
        match.setResumeId("resume-101");
        match.setUserId("user-101");
        match.setJobTitle("Java Developer");
        match.setCompanyName("TechNova");
        match.setLocation("Remote");
        match.setExternalJobId("external-101");
        match.setApplyUrl("https://linkedin.com/jobs/101");
        match.setJobDescription("Need Java and Spring Boot");
        match.setMatchScore(score);
        match.setMissingSkills("AWS,Docker");
        match.setRecommendations("Add cloud projects");
        match.setSource(source);
        match.setIsBookmarked(false);
        match.setMatchedAt(LocalDateTime.now());
        return match;
    }
}
