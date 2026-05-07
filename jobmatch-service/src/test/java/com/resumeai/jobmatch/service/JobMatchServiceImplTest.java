package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.client.AiServiceClient;
import com.resumeai.jobmatch.client.AuthServiceClient;
import com.resumeai.jobmatch.client.LinkedInJobClient;
import com.resumeai.jobmatch.client.NotificationServiceClient;
import com.resumeai.jobmatch.client.ResumeServiceClient;
import com.resumeai.jobmatch.client.SectionServiceClient;
import com.resumeai.jobmatch.dto.AiJobFitResponse;
import com.resumeai.jobmatch.dto.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.ResumeResponse;
import com.resumeai.jobmatch.dto.SectionResponse;
import com.resumeai.jobmatch.dto.UserResponse;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobMatchServiceImplTest {

    private JobMatchRepository repository;
    private AuthServiceClient authServiceClient;
    private ResumeServiceClient resumeServiceClient;
    private SectionServiceClient sectionServiceClient;
    private AiServiceClient aiServiceClient;
    private NotificationServiceClient notificationServiceClient;
    private LinkedInJobClient linkedInJobClient;
    private JobMatchServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(JobMatchRepository.class);
        authServiceClient = mock(AuthServiceClient.class);
        resumeServiceClient = mock(ResumeServiceClient.class);
        sectionServiceClient = mock(SectionServiceClient.class);
        aiServiceClient = mock(AiServiceClient.class);
        notificationServiceClient = mock(NotificationServiceClient.class);
        linkedInJobClient = mock(LinkedInJobClient.class);

        service = new JobMatchServiceImpl(
                repository,
                authServiceClient,
                resumeServiceClient,
                sectionServiceClient,
                aiServiceClient,
                notificationServiceClient,
                linkedInJobClient
        );

        ReflectionTestUtils.setField(service, "notificationThreshold", 75);
    }

    @Test
    void analyzeJobFitShouldSaveMatch() {
        when(authServiceClient.getUserById("u1")).thenReturn(
                new UserResponse("u1", "Test", "a@b.com", "999", "USER", "LOCAL", true, "PREMIUM", null)
        );
        when(resumeServiceClient.getResumeById("r1")).thenReturn(
                new ResumeResponse("r1", "u1", "Java Resume", "Java Developer", "t1", 80, "DRAFT", "EN", false, 0, null, null)
        );
        when(sectionServiceClient.getSectionsByResume("r1")).thenReturn(
                List.of(new SectionResponse("s1", "r1", "SUMMARY", "Summary", "Strong Java profile", 1, true, false, null, null))
        );
        when(aiServiceClient.analyzeJobFit(any())).thenReturn(new AiJobFitResponse(88, "AWS,Docker", "Add cloud achievements"));
        when(repository.save(any(JobMatch.class))).thenAnswer(invocation -> {
            JobMatch jobMatch = invocation.getArgument(0);
            jobMatch.setMatchId("m1");
            jobMatch.setMatchedAt(LocalDateTime.now());
            return jobMatch;
        });

        var response = service.analyzeJobFit(new AnalyzeJobFitRequest("r1", "u1", "Senior Java Developer", "Spring Boot AWS Kafka"));

        assertThat(response.matchId()).isEqualTo("m1");
        assertThat(response.matchScore()).isEqualTo(88);
        verify(notificationServiceClient).sendNotification(any());
    }
}