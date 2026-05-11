package com.resumeai.export.service;

import com.resumeai.export.client.AuthServiceClient;
import com.resumeai.export.client.ResumeServiceClient;
import com.resumeai.export.client.SectionServiceClient;
import com.resumeai.export.client.TemplateServiceClient;
import com.resumeai.export.dto.AuthUserResponse;
import com.resumeai.export.dto.DownloadLinkResponse;
import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;
import com.resumeai.export.dto.ResumeSummaryResponse;
import com.resumeai.export.dto.SectionResponse;
import com.resumeai.export.dto.TemplateResponse;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.enums.ExportStatus;
import com.resumeai.export.exception.BadRequestException;
import com.resumeai.export.exception.ResourceNotFoundException;
import com.resumeai.export.messaging.ExportMessage;
import com.resumeai.export.repository.ExportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private ExportRepository exportRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LocalStorageService localStorageService;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @Mock
    private SectionServiceClient sectionServiceClient;

    @Mock
    private TemplateServiceClient templateServiceClient;

    @Mock
    private ExportContentBuilderService exportContentBuilderService;

    @Mock
    private ExportWorker exportWorker;

    private ExportServiceImpl exportService;

    private AuthUserResponse freeUser;
    private AuthUserResponse premiumUser;
    private AuthUserResponse inactiveUser;
    private ResumeSummaryResponse resume;
    private TemplateResponse template;
    private List<SectionResponse> sections;

    @BeforeEach
    void setUp() {
        exportService = new ExportServiceImpl(
                exportRepository,
                rabbitTemplate,
                localStorageService,
                authServiceClient,
                resumeServiceClient,
                sectionServiceClient,
                templateServiceClient,
                exportContentBuilderService,
                exportWorker
        );

        ReflectionTestUtils.setField(exportService, "expiryDays", 7);
        ReflectionTestUtils.setField(exportService, "exportProcessingMode", "async");

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
                80,
                "DRAFT",
                "English",
                false,
                0,
                null,
                null
        );

        template = new TemplateResponse(
                "template-101",
                "Modern",
                "Modern template",
                "thumb.png",
                "<html>{{sections}}</html>",
                "body { font-family: Arial; }",
                "MODERN",
                false,
                true,
                5,
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
    void submitExportShouldQueuePdfExportForFreeUser() {
        mockValidExportDependencies(freeUser);
        when(exportRepository.countByUserIdAndFormatAndRequestedAtBetween(
                eq("user-101"),
                eq(ExportFormat.PDF),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0L);
        mockContentBuilder();
        mockSave();

        ExportRequest request = exportRequest(ExportFormat.PDF, null, null, null);

        ExportJobResponse response = exportService.submitExport(request);

        assertNotNull(response.jobId());
        assertEquals("resume-101", response.resumeId());
        assertEquals("user-101", response.userId());
        assertEquals(ExportFormat.PDF, response.format());
        assertEquals(ExportStatus.QUEUED, response.status());
        assertEquals("template-101", response.templateId());

        verify(exportRepository).save(any(ExportJob.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(ExportMessage.class));
        verify(exportWorker, never()).processExportJob(anyString());
    }

    @Test
    void submitExportShouldUseProvidedSnapshotsWhenPresent() {
        mockValidExportDependencies(freeUser);
        when(exportRepository.countByUserIdAndFormatAndRequestedAtBetween(
                eq("user-101"),
                eq(ExportFormat.PDF),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0L);
        mockSave();

        ExportRequest request = exportRequest(
                ExportFormat.PDF,
                "{\"provided\":true}",
                "<html>Provided</html>",
                "body{}"
        );

        ExportJobResponse response = exportService.submitExport(request);

        assertEquals(ExportStatus.QUEUED, response.status());
        verify(exportContentBuilderService, never()).buildResumeJson(any(), any(), any());
        verify(exportContentBuilderService, never()).buildHtml(any(), any(), any());
        verify(exportContentBuilderService, never()).buildCss(any());
    }

    @Test
    void submitExportShouldRejectInactiveUser() {
        when(authServiceClient.getUserById("user-101")).thenReturn(inactiveUser);

        ExportRequest request = exportRequest(ExportFormat.PDF, null, null, null);

        assertThrows(BadRequestException.class, () -> exportService.submitExport(request));
        verify(exportRepository, never()).save(any());
    }

    @Test
    void submitExportShouldRejectWrongResumeOwner() {
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

        ExportRequest request = exportRequest(ExportFormat.PDF, null, null, null);

        assertThrows(BadRequestException.class, () -> exportService.submitExport(request));
        verify(exportRepository, never()).save(any());
    }

    @Test
    void submitExportShouldRejectDocxForFreeUser() {
        mockValidExportDependencies(freeUser);

        ExportRequest request = exportRequest(ExportFormat.DOCX, null, null, null);

        assertThrows(BadRequestException.class, () -> exportService.submitExport(request));
        verify(exportRepository, never()).save(any());
    }

    @Test
    void submitExportShouldAllowDocxForPremiumUser() {
        mockValidExportDependencies(premiumUser);
        mockContentBuilder();
        mockSave();

        ExportRequest request = exportRequest(ExportFormat.DOCX, null, null, null);

        ExportJobResponse response = exportService.submitExport(request);

        assertEquals(ExportFormat.DOCX, response.format());
        assertEquals(ExportStatus.QUEUED, response.status());
    }

    @Test
    void submitExportShouldRejectFreePdfWhenDailyQuotaExceeded() {
        mockValidExportDependencies(freeUser);
        when(exportRepository.countByUserIdAndFormatAndRequestedAtBetween(
                eq("user-101"),
                eq(ExportFormat.PDF),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(10L);

        ExportRequest request = exportRequest(ExportFormat.PDF, null, null, null);

        assertThrows(BadRequestException.class, () -> exportService.submitExport(request));
        verify(exportRepository, never()).save(any());
    }

    @Test
    void getJobStatusShouldReturnJob() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));

        ExportJobResponse response = exportService.getJobStatus("job-101");

        assertEquals("job-101", response.jobId());
        assertEquals(ExportStatus.COMPLETED, response.status());
    }

    @Test
    void getJobStatusShouldThrowWhenMissing() {
        when(exportRepository.findByJobId("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> exportService.getJobStatus("missing"));
    }

    @Test
    void getExportsByUserShouldReturnUserExports() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(exportRepository.findByUserId("user-101")).thenReturn(List.of(job));

        List<ExportJobResponse> responses = exportService.getExportsByUser("user-101");

        assertEquals(1, responses.size());
        assertEquals("job-101", responses.get(0).jobId());
    }

    @Test
    void generateDownloadLinkShouldCreateNewTokenForCompletedJob() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        job.setFileUrl("C:/exports/job-101.pdf");

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));
        when(exportRepository.save(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DownloadLinkResponse response = exportService.generateDownloadLink("job-101", "user-101");

        assertEquals("job-101", response.jobId());
        assertTrue(response.downloadUrl().startsWith("/exports/download/"));
        assertNotNull(response.expiresAt());
        verify(exportRepository).save(job);
    }

    @Test
    void generateDownloadLinkShouldReuseValidToken() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        job.setFileUrl("C:/exports/job-101.pdf");
        job.setDownloadToken("token-101");
        job.setDownloadTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));

        DownloadLinkResponse response = exportService.generateDownloadLink("job-101", "user-101");

        assertEquals("/exports/download/token-101", response.downloadUrl());
        verify(exportRepository, never()).save(any());
    }

    @Test
    void generateDownloadLinkShouldRejectWrongOwner() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        job.setUserId("other-user");
        job.setFileUrl("C:/exports/job-101.pdf");

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class,
                () -> exportService.generateDownloadLink("job-101", "user-101"));
    }

    @Test
    void generateDownloadLinkShouldRejectWhenFileNotReady() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.QUEUED);

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class,
                () -> exportService.generateDownloadLink("job-101", "user-101"));
    }

    @Test
    void getDownloadPathByTokenShouldReturnFilePath() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        job.setFileUrl("C:/exports/job-101.pdf");
        job.setDownloadToken("token-101");
        job.setDownloadTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(exportRepository.findByDownloadToken("token-101")).thenReturn(Optional.of(job));

        String path = exportService.getDownloadPathByToken("token-101");

        assertEquals("C:/exports/job-101.pdf", path);
    }

    @Test
    void getDownloadPathByTokenShouldRejectExpiredToken() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        job.setFileUrl("C:/exports/job-101.pdf");
        job.setDownloadToken("token-101");
        job.setDownloadTokenExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(exportRepository.findByDownloadToken("token-101")).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class,
                () -> exportService.getDownloadPathByToken("token-101"));
    }

    @Test
    void deleteExportShouldDeleteFileAndJob() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        job.setFileUrl("C:/exports/job-101.pdf");

        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));

        exportService.deleteExport("job-101");

        verify(localStorageService).deleteFile("C:/exports/job-101.pdf");
        verify(exportRepository).delete(job);
    }

    @Test
    void deleteExportShouldDeleteJobWhenNoFileUrl() {
        ExportJob job = exportJob("job-101", ExportFormat.PDF, ExportStatus.FAILED);
        job.setFileUrl(null);

        when(exportRepository.findByJobId("job-101")).thenReturn(Optional.of(job));

        exportService.deleteExport("job-101");

        verify(localStorageService, never()).deleteFile(anyString());
        verify(exportRepository).delete(job);
    }

    @Test
    void cleanupExpiredExportsShouldDeleteExpiredFilesAndJobs() {
        ExportJob withFile = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        withFile.setFileUrl("C:/exports/job-101.pdf");

        ExportJob withoutFile = exportJob("job-102", ExportFormat.JSON, ExportStatus.FAILED);
        withoutFile.setFileUrl(null);

        when(exportRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(withFile, withoutFile));

        exportService.cleanupExpiredExports();

        verify(localStorageService).deleteFile("C:/exports/job-101.pdf");
        verify(exportRepository).delete(withFile);
        verify(exportRepository).delete(withoutFile);
    }

    @Test
    void getExportStatsShouldReturnAllExports() {
        ExportJob pdf = exportJob("job-101", ExportFormat.PDF, ExportStatus.COMPLETED);
        ExportJob json = exportJob("job-102", ExportFormat.JSON, ExportStatus.FAILED);

        when(exportRepository.findAll()).thenReturn(List.of(pdf, json));

        List<ExportJobResponse> responses = exportService.getExportStats();

        assertEquals(2, responses.size());
        assertEquals("job-101", responses.get(0).jobId());
        assertEquals("job-102", responses.get(1).jobId());
    }

    private void mockValidExportDependencies(AuthUserResponse user) {
        when(authServiceClient.getUserById("user-101")).thenReturn(user);
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(template);
        when(sectionServiceClient.getSectionsByResume("resume-101")).thenReturn(sections);
    }

    private void mockContentBuilder() {
        when(exportContentBuilderService.buildResumeJson(resume, sections, "blue-theme"))
                .thenReturn("{\"resumeId\":\"resume-101\"}");
        when(exportContentBuilderService.buildHtml(resume, sections, template))
                .thenReturn("<html>Resume</html>");
        when(exportContentBuilderService.buildCss(template))
                .thenReturn("body{}");
    }

    private void mockSave() {
        when(exportRepository.save(any(ExportJob.class))).thenAnswer(invocation -> {
            ExportJob job = invocation.getArgument(0);
            if (job.getJobId() == null) {
                job.prePersist();
            }
            return job;
        });
    }

    private ExportRequest exportRequest(ExportFormat format,
                                        String resumeDataJson,
                                        String htmlSnapshot,
                                        String cssSnapshot) {
        return new ExportRequest(
                "resume-101",
                "user-101",
                format,
                "template-101",
                "blue-theme",
                resumeDataJson,
                htmlSnapshot,
                cssSnapshot
        );
    }

    private ExportJob exportJob(String jobId, ExportFormat format, ExportStatus status) {
        ExportJob job = new ExportJob();
        job.setJobId(jobId);
        job.setResumeId("resume-101");
        job.setUserId("user-101");
        job.setFormat(format);
        job.setStatus(status);
        job.setTemplateId("template-101");
        job.setCustomizations("blue-theme");
        job.setResumeDataJson("{\"resumeId\":\"resume-101\"}");
        job.setHtmlSnapshot("<html>Resume</html>");
        job.setCssSnapshot("body{}");
        job.setRequestedAt(LocalDateTime.now().minusMinutes(10));
        job.setExpiresAt(LocalDateTime.now().plusDays(7));
        if (status == ExportStatus.COMPLETED) {
            job.setCompletedAt(LocalDateTime.now());
            job.setFileSizeKb(25);
        }
        return job;
    }
}
