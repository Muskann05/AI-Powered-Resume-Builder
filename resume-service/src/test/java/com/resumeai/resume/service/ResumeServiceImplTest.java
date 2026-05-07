package com.resumeai.resume.service;

import com.resumeai.resume.client.AuthServiceClient;
import com.resumeai.resume.client.SectionServiceClient;
import com.resumeai.resume.client.TemplateServiceClient;
import com.resumeai.resume.dto.AuthUserResponse;
import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.SectionResponse;
import com.resumeai.resume.dto.TemplateResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.enums.ResumeStatus;
import com.resumeai.resume.exception.BadRequestException;
import com.resumeai.resume.exception.ResourceNotFoundException;
import com.resumeai.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private TemplateServiceClient templateServiceClient;

    @Mock
    private SectionServiceClient sectionServiceClient;

    private ResumeServiceImpl resumeService;
    private Resume resume;
    private AuthUserResponse freeUser;
    private AuthUserResponse premiumUser;
    private TemplateResponse freeTemplate;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeServiceImpl(
                resumeRepository,
                authServiceClient,
                templateServiceClient,
                sectionServiceClient
        );

        resume = new Resume();
        resume.setResumeId("resume-101");
        resume.setUserId("user-101");
        resume.setTitle("Backend Resume");
        resume.setTargetJobTitle("Java Developer");
        resume.setTemplateId("template-101");
        resume.setAtsScore(80);
        resume.setStatus(ResumeStatus.DRAFT);
        resume.setLanguage("English");
        resume.setIsPublic(false);
        resume.setViewCount(0);

        freeUser = new AuthUserResponse(
                "user-101",
                "Muskan",
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
                "Muskan",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                true,
                "PREMIUM",
                null
        );

        freeTemplate = new TemplateResponse(
                "template-101",
                "Modern",
                "Modern resume template",
                "thumb.png",
                "<html></html>",
                "body{}",
                "MODERN",
                false,
                true,
                0,
                null
        );
    }

    @Test
    void createResumeShouldPersistResumeForFreeUserBelowLimit() {
        CreateResumeRequest request = new CreateResumeRequest(
                "user-101",
                "Backend Resume",
                "Java Developer",
                "template-101",
                "English"
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(freeTemplate);
        when(resumeRepository.countByUserId("user-101")).thenReturn(1L);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume saved = invocation.getArgument(0);
            saved.setResumeId("resume-101");
            return saved;
        });

        var response = resumeService.createResume(request);

        assertEquals("resume-101", response.resumeId());
        assertEquals("user-101", response.userId());
        assertEquals("Backend Resume", response.title());
        assertEquals(ResumeStatus.DRAFT, response.status());
        assertFalse(response.isPublic());

        verify(templateServiceClient).incrementUsage("template-101");
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void createResumeShouldRejectInactiveUser() {
        AuthUserResponse inactiveUser = new AuthUserResponse(
                "user-101",
                "Muskan",
                "muskan@example.com",
                "9999999999",
                "USER",
                "LOCAL",
                false,
                "FREE",
                null
        );

        CreateResumeRequest request = new CreateResumeRequest(
                "user-101",
                "Backend Resume",
                "Java Developer",
                "template-101",
                "English"
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(inactiveUser);

        assertThrows(BadRequestException.class, () -> resumeService.createResume(request));
    }

    @Test
    void createResumeShouldRejectUnavailableTemplate() {
        CreateResumeRequest request = new CreateResumeRequest(
                "user-101",
                "Backend Resume",
                "Java Developer",
                "template-101",
                "English"
        );

        TemplateResponse inactiveTemplate = new TemplateResponse(
                "template-101",
                "Modern",
                "Modern resume template",
                "thumb.png",
                "<html></html>",
                "body{}",
                "MODERN",
                false,
                false,
                0,
                null
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(inactiveTemplate);

        assertThrows(BadRequestException.class, () -> resumeService.createResume(request));
    }

    @Test
    void createResumeShouldRejectFreeUserWhenLimitReached() {
        CreateResumeRequest request = new CreateResumeRequest(
                "user-101",
                "Backend Resume",
                "Java Developer",
                "template-101",
                "English"
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(freeTemplate);
        when(resumeRepository.countByUserId("user-101")).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> resumeService.createResume(request));
    }

    @Test
    void createResumeShouldAllowPremiumUserBeyondFreeLimit() {
        CreateResumeRequest request = new CreateResumeRequest(
                "user-101",
                "Premium Resume",
                "Senior Java Developer",
                "template-101",
                "English"
        );

        when(authServiceClient.getUserById("user-101")).thenReturn(premiumUser);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(freeTemplate);
        when(resumeRepository.countByUserId("user-101")).thenReturn(20L);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume saved = invocation.getArgument(0);
            saved.setResumeId("resume-premium");
            return saved;
        });

        var response = resumeService.createResume(request);

        assertEquals("resume-premium", response.resumeId());
    }

    @Test
    void getResumeByIdShouldReturnResume() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));

        var response = resumeService.getResumeById("resume-101");

        assertEquals("resume-101", response.resumeId());
        assertEquals("Backend Resume", response.title());
    }

    @Test
    void getResumeByIdShouldThrowWhenMissing() {
        when(resumeRepository.findByResumeId("missing-resume")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resumeService.getResumeById("missing-resume"));
    }

    @Test
    void getResumesByUserShouldValidateUserAndReturnResumes() {
        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(resumeRepository.findByUserId("user-101")).thenReturn(List.of(resume));

        var responses = resumeService.getResumesByUser("user-101");

        assertEquals(1, responses.size());
        assertEquals("resume-101", responses.get(0).resumeId());
    }

    @Test
    void updateResumeShouldModifyMutableFieldsWithoutChangingTemplate() {
        UpdateResumeRequest request = new UpdateResumeRequest(
                "Updated Resume",
                "Senior Java Developer",
                "template-101",
                "Hindi",
                ResumeStatus.COMPLETE
        );

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.updateResume("resume-101", request);

        assertEquals("Updated Resume", response.title());
        assertEquals("Senior Java Developer", response.targetJobTitle());
        assertEquals("template-101", response.templateId());
        assertEquals("Hindi", response.language());
        assertEquals(ResumeStatus.COMPLETE, response.status());
    }

    @Test
    void updateResumeShouldValidateNewTemplateWhenTemplateChanges() {
        UpdateResumeRequest request = new UpdateResumeRequest(
                "Updated Resume",
                "Senior Java Developer",
                "template-202",
                "English",
                ResumeStatus.COMPLETE
        );

        TemplateResponse newTemplate = new TemplateResponse(
                "template-202",
                "Creative",
                "Creative template",
                "creative.png",
                "<html></html>",
                "body{}",
                "CREATIVE",
                false,
                true,
                0,
                null
        );

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(templateServiceClient.validateTemplateAccess("template-202", "user-101")).thenReturn(newTemplate);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.updateResume("resume-101", request);

        assertEquals("template-202", response.templateId());
        verify(templateServiceClient).incrementUsage("template-202");
    }

    @Test
    void updateResumeShouldRejectUnavailableNewTemplate() {
        UpdateResumeRequest request = new UpdateResumeRequest(
                "Updated Resume",
                "Senior Java Developer",
                "template-202",
                "English",
                ResumeStatus.COMPLETE
        );

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(templateServiceClient.validateTemplateAccess("template-202", "user-101")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> resumeService.updateResume("resume-101", request));
    }

    @Test
    void deleteResumeShouldDeleteSectionsAndResume() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));

        resumeService.deleteResume("resume-101");

        verify(sectionServiceClient).deleteAllSections("resume-101");
        verify(resumeRepository).delete(resume);
    }

    @Test
    void duplicateResumeShouldCreateCopyAndCloneSections() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(freeTemplate);
        when(resumeRepository.countByUserId("user-101")).thenReturn(1L);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume copy = invocation.getArgument(0);
            copy.setResumeId("resume-copy");
            return copy;
        });

        var response = resumeService.duplicateResume("resume-101");

        assertEquals("resume-copy", response.resumeId());
        assertEquals("Backend Resume Copy", response.title());
        assertFalse(response.isPublic());
        assertEquals(ResumeStatus.DRAFT, response.status());

        verify(sectionServiceClient).cloneSections("resume-101", "resume-copy");
        verify(templateServiceClient).incrementUsage("template-101");
    }

    @Test
    void duplicateResumeShouldRejectFreeUserWhenLimitReached() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(authServiceClient.getUserById("user-101")).thenReturn(freeUser);
        when(templateServiceClient.validateTemplateAccess("template-101", "user-101")).thenReturn(freeTemplate);
        when(resumeRepository.countByUserId("user-101")).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> resumeService.duplicateResume("resume-101"));
    }

    @Test
    void updateAtsScoreShouldPersistScore() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.updateAtsScore("resume-101", 92);

        assertEquals(92, response.atsScore());
    }

    @Test
    void publishResumeShouldRequireAtLeastOneVisibleSectionWithContent() {
        SectionResponse section = new SectionResponse(
                "section-101",
                "resume-101",
                "SUMMARY",
                "Summary",
                "Experienced Java developer",
                1,
                true,
                false,
                null,
                null
        );

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(sectionServiceClient.getSectionsByResume("resume-101")).thenReturn(List.of(section));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.publishResume("resume-101");

        assertTrue(response.isPublic());
        assertEquals(ResumeStatus.COMPLETE, response.status());
    }

    @Test
    void publishResumeShouldRejectWhenNoSectionsExist() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(sectionServiceClient.getSectionsByResume("resume-101")).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> resumeService.publishResume("resume-101"));
    }

    @Test
    void publishResumeShouldRejectWhenNoVisibleContentExists() {
        SectionResponse hiddenSection = new SectionResponse(
                "section-101",
                "resume-101",
                "SUMMARY",
                "Summary",
                "Hidden content",
                1,
                false,
                false,
                null,
                null
        );

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(sectionServiceClient.getSectionsByResume("resume-101")).thenReturn(List.of(hiddenSection));

        assertThrows(BadRequestException.class, () -> resumeService.publishResume("resume-101"));
    }

    @Test
    void unpublishResumeShouldMarkPrivateAndDraft() {
        resume.setIsPublic(true);
        resume.setStatus(ResumeStatus.COMPLETE);

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.unpublishResume("resume-101");

        assertFalse(response.isPublic());
        assertEquals(ResumeStatus.DRAFT, response.status());
    }

    @Test
    void getPublicResumeShouldIncreaseViewCount() {
        resume.setIsPublic(true);
        resume.setViewCount(5);

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.getPublicResume("resume-101");

        assertEquals(6, response.viewCount());
    }

    @Test
    void getPublicResumeShouldRejectPrivateResume() {
        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));

        assertThrows(ResourceNotFoundException.class, () -> resumeService.getPublicResume("resume-101"));
    }

    @Test
    void getPublicResumesShouldReturnTopPublicGallery() {
        resume.setIsPublic(true);
        when(resumeRepository.findTop12ByIsPublicTrueOrderByViewCountDescCreatedAtDesc()).thenReturn(List.of(resume));

        var responses = resumeService.getPublicResumes();

        assertEquals(1, responses.size());
        assertEquals("resume-101", responses.get(0).resumeId());
    }

    @Test
    void searchPublicResumesShouldReturnGalleryWhenKeywordBlank() {
        resume.setIsPublic(true);
        when(resumeRepository.findTop12ByIsPublicTrueOrderByViewCountDescCreatedAtDesc()).thenReturn(List.of(resume));

        var responses = resumeService.searchPublicResumes(" ");

        assertEquals(1, responses.size());
    }

    @Test
    void searchPublicResumesShouldSearchByKeyword() {
        resume.setIsPublic(true);
        when(resumeRepository.findByIsPublicTrueAndTargetJobTitleContainingIgnoreCaseOrderByViewCountDescCreatedAtDesc("Java"))
                .thenReturn(List.of(resume));

        var responses = resumeService.searchPublicResumes(" Java ");

        assertEquals(1, responses.size());
        assertEquals("Java Developer", responses.get(0).targetJobTitle());
    }

    @Test
    void incrementViewCountShouldIncreaseViewCount() {
        resume.setViewCount(3);

        when(resumeRepository.findByResumeId("resume-101")).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.incrementViewCount("resume-101");

        assertEquals(4, response.viewCount());
    }

    @Test
    void getResumesByTemplateShouldReturnResumes() {
        when(resumeRepository.findByTemplateId("template-101")).thenReturn(List.of(resume));

        var responses = resumeService.getResumesByTemplate("template-101");

        assertEquals(1, responses.size());
        assertEquals("template-101", responses.get(0).templateId());
    }

    @Test
    void getAllResumesForAdminShouldReturnAllResumes() {
        when(resumeRepository.findAll()).thenReturn(List.of(resume));

        var responses = resumeService.getAllResumesForAdmin();

        assertEquals(1, responses.size());
        assertEquals("resume-101", responses.get(0).resumeId());
    }

    @Test
    void getAdminStatsShouldReturnResumeCountsAndViews() {
        Resume publicCompleteResume = new Resume();
        publicCompleteResume.setResumeId("resume-202");
        publicCompleteResume.setUserId("user-202");
        publicCompleteResume.setTitle("Public Resume");
        publicCompleteResume.setTargetJobTitle("Frontend Developer");
        publicCompleteResume.setTemplateId("template-202");
        publicCompleteResume.setStatus(ResumeStatus.COMPLETE);
        publicCompleteResume.setIsPublic(true);
        publicCompleteResume.setViewCount(12);

        resume.setStatus(ResumeStatus.DRAFT);
        resume.setIsPublic(false);
        resume.setViewCount(5);

        when(resumeRepository.findAll()).thenReturn(List.of(resume, publicCompleteResume));

        var stats = resumeService.getAdminStats();

        assertEquals(2, stats.totalResumes());
        assertEquals(1, stats.publicResumes());
        assertEquals(1, stats.draftResumes());
        assertEquals(1, stats.completeResumes());
        assertEquals(17, stats.totalViews());
    }
}
