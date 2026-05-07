package com.resumeai.export.service;

import com.resumeai.export.client.AuthServiceClient;
import com.resumeai.export.client.ResumeServiceClient;
import com.resumeai.export.client.SectionServiceClient;
import com.resumeai.export.client.TemplateServiceClient;
import com.resumeai.export.dto.AuthUserResponse;
import com.resumeai.export.dto.ExportRequest;
import com.resumeai.export.dto.ResumeSummaryResponse;
import com.resumeai.export.dto.SectionResponse;
import com.resumeai.export.dto.TemplateResponse;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.exception.BadRequestException;
import com.resumeai.export.repository.ExportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ExportServiceImpl exportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "expiryDays", 7);

        when(authServiceClient.getUserById("user-101")).thenReturn(
                new AuthUserResponse("user-101", "Muskan", "muskan@example.com", "9999999999", "USER", "LOCAL", true, "FREE", null)
        );
        when(authServiceClient.getUserById("user-202")).thenReturn(
                new AuthUserResponse("user-202", "Premium User", "premium@example.com", "9999999999", "USER", "LOCAL", true, "PREMIUM", null)
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(
                new ResumeSummaryResponse("resume-101", "user-101", "Backend Resume", "Java Developer", "template-101", 88, "DRAFT", "English", false, 0, null, null)
        );
        when(resumeServiceClient.getResumeById("resume-202")).thenReturn(
                new ResumeSummaryResponse("resume-202", "user-202", "Premium Resume", "Java Developer", "template-202", 90, "DRAFT", "English", false, 0, null, null)
        );

        when(sectionServiceClient.getSectionsByResume("resume-101")).thenReturn(List.of(
                new SectionResponse("section-1", "resume-101", "SUMMARY", "Summary", "Java developer", 1, true, false, null, null)
        ));
        when(sectionServiceClient.getSectionsByResume("resume-202")).thenReturn(List.of(
                new SectionResponse("section-2", "resume-202", "SUMMARY", "Summary", "Senior developer", 1, true, false, null, null)
        ));

        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(
                new TemplateResponse("template-101", "Modern", "desc", "thumb", "<html>{{sections}}</html>", "body{}", "MODERN", false, true, 1, null)
        );
        when(templateServiceClient.validateTemplateAccess("template-202", "user-202")).thenReturn(
                new TemplateResponse("template-202", "Premium", "desc", "thumb", "<html>{{sections}}</html>", "body{}", "MODERN", true, true, 1, null)
        );

        when(exportContentBuilderService.buildResumeJson(any(), any(), any())).thenReturn("{\"ok\":true}");
        when(exportContentBuilderService.buildHtml(any(), any(), any())).thenReturn("<html>resume</html>");
        when(exportContentBuilderService.buildCss(any())).thenReturn("body{}");
        when(exportRepository.save(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitExportShouldQueueFreePdfExport() {
        when(exportRepository.countByUserIdAndFormatAndRequestedAtBetween(any(), any(), any(), any())).thenReturn(2L);

        var response = exportService.submitExport(new ExportRequest(
                "resume-101",
                "user-101",
                ExportFormat.PDF,
                "template-101",
                null,
                null,
                null,
                null
        ));

        assertEquals(ExportFormat.PDF, response.format());
        verify(exportRepository).save(any(ExportJob.class));
    }

    @Test
    void submitExportShouldRejectFreeDocxExport() {
        assertThrows(BadRequestException.class, () -> exportService.submitExport(new ExportRequest(
                "resume-101",
                "user-101",
                ExportFormat.DOCX,
                "template-101",
                null,
                null,
                null,
                null
        )));
    }

    @Test
    void submitExportShouldAllowPremiumJsonExport() {
        var response = exportService.submitExport(new ExportRequest(
                "resume-202",
                "user-202",
                ExportFormat.JSON,
                "template-202",
                null,
                null,
                null,
                null
        ));

        assertEquals(ExportFormat.JSON, response.format());
    }

    @Test
    void submitExportShouldRejectMismatchedResumeOwnership() {
        when(resumeServiceClient.getResumeById("resume-303")).thenReturn(
                new ResumeSummaryResponse("resume-303", "user-other", "Resume", "Java Developer", "template-101", 80, "DRAFT", "English", false, 0, null, null)
        );

        assertThrows(BadRequestException.class, () -> exportService.submitExport(new ExportRequest(
                "resume-303",
                "user-101",
                ExportFormat.PDF,
                "template-101",
                null,
                null,
                null,
                null
        )));
    }

    @Test
    void submitExportShouldRejectFreePdfQuotaOverflow() {
        when(exportRepository.countByUserIdAndFormatAndRequestedAtBetween(any(), any(), any(), any())).thenReturn(10L);

        assertThrows(BadRequestException.class, () -> exportService.submitExport(new ExportRequest(
                "resume-101",
                "user-101",
                ExportFormat.PDF,
                "template-101",
                null,
                null,
                null,
                null
        )));
    }
}
