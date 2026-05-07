package com.resumeai.section.service;

import com.resumeai.section.client.ResumeServiceClient;
import com.resumeai.section.dto.BulkUpdateSectionsRequest;
import com.resumeai.section.dto.CreateSectionRequest;
import com.resumeai.section.dto.ReorderSectionsRequest;
import com.resumeai.section.dto.ResumeSummaryResponse;
import com.resumeai.section.dto.UpdateSectionRequest;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.enums.SectionType;
import com.resumeai.section.exception.BadRequestException;
import com.resumeai.section.exception.ResourceNotFoundException;
import com.resumeai.section.repository.SectionRepository;
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
class SectionServiceImplTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    private SectionServiceImpl sectionService;
    private ResumeSection section;
    private ResumeSummaryResponse resume;

    @BeforeEach
    void setUp() {
        sectionService = new SectionServiceImpl(sectionRepository, resumeServiceClient);

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

        section = new ResumeSection();
        section.setSectionId("section-101");
        section.setResumeId("resume-101");
        section.setSectionType(SectionType.SUMMARY);
        section.setTitle("Professional Summary");
        section.setContent("Experienced Java developer");
        section.setDisplayOrder(1);
        section.setIsVisible(true);
        section.setAiGenerated(false);
    }

    @Test
    void addSectionShouldCreateSectionWithRequestedOrder() {
        CreateSectionRequest request = new CreateSectionRequest(
                "resume-101",
                SectionType.SUMMARY,
                "Professional Summary",
                "Experienced Java developer",
                2,
                true,
                false
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection saved = invocation.getArgument(0);
            saved.setSectionId("section-new");
            return saved;
        });

        var response = sectionService.addSection(request);

        assertEquals("section-new", response.sectionId());
        assertEquals("resume-101", response.resumeId());
        assertEquals(SectionType.SUMMARY, response.sectionType());
        assertEquals(2, response.displayOrder());
        assertTrue(response.isVisible());
        assertFalse(response.aiGenerated());
    }

    @Test
    void addSectionShouldResolveOrderWhenOrderMissing() {
        CreateSectionRequest request = new CreateSectionRequest(
                "resume-101",
                SectionType.SKILLS,
                "Skills",
                "Java, Spring Boot",
                null,
                null,
                null
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.countByResumeId("resume-101")).thenReturn(3L);
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection saved = invocation.getArgument(0);
            saved.setSectionId("section-new");
            return saved;
        });

        var response = sectionService.addSection(request);

        assertEquals(4, response.displayOrder());
        assertTrue(response.isVisible());
        assertFalse(response.aiGenerated());
    }

    @Test
    void addSectionShouldRejectBlankResumeId() {
        CreateSectionRequest request = new CreateSectionRequest(
                "",
                SectionType.SUMMARY,
                "Professional Summary",
                "Experienced Java developer",
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> sectionService.addSection(request));
    }

    @Test
    void getSectionsByResumeShouldReturnOrderedSections() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101")).thenReturn(List.of(section));

        var response = sectionService.getSectionsByResume("resume-101");

        assertEquals(1, response.size());
        assertEquals("section-101", response.get(0).sectionId());
    }

    @Test
    void getSectionByIdShouldReturnSection() {
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));

        var response = sectionService.getSectionById("section-101");

        assertEquals("section-101", response.sectionId());
        assertEquals("Professional Summary", response.title());
    }

    @Test
    void getSectionByIdShouldThrowWhenMissing() {
        when(sectionRepository.findBySectionId("missing-section")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sectionService.getSectionById("missing-section"));
    }

    @Test
    void updateSectionShouldUpdateMutableFields() {
        UpdateSectionRequest request = new UpdateSectionRequest(
                SectionType.EXPERIENCE,
                "Work Experience",
                "Built backend APIs",
                5,
                false,
                true
        );

        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.updateSection("section-101", request);

        assertEquals(SectionType.EXPERIENCE, response.sectionType());
        assertEquals("Work Experience", response.title());
        assertEquals("Built backend APIs", response.content());
        assertEquals(5, response.displayOrder());
        assertFalse(response.isVisible());
        assertTrue(response.aiGenerated());
    }

    @Test
    void updateSectionShouldKeepOptionalFieldsWhenNull() {
        UpdateSectionRequest request = new UpdateSectionRequest(
                SectionType.SUMMARY,
                "Updated Summary",
                "Updated content",
                null,
                null,
                null
        );

        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.updateSection("section-101", request);

        assertEquals(1, response.displayOrder());
        assertTrue(response.isVisible());
        assertFalse(response.aiGenerated());
    }

    @Test
    void deleteSectionShouldDeleteSection() {
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));

        sectionService.deleteSection("section-101");

        verify(sectionRepository).delete(section);
    }

    @Test
    void reorderSectionsShouldUpdateDisplayOrders() {
        ResumeSection sectionTwo = new ResumeSection();
        sectionTwo.setSectionId("section-202");
        sectionTwo.setResumeId("resume-101");
        sectionTwo.setSectionType(SectionType.SKILLS);
        sectionTwo.setTitle("Skills");
        sectionTwo.setContent("Java");
        sectionTwo.setDisplayOrder(2);
        sectionTwo.setIsVisible(true);
        sectionTwo.setAiGenerated(false);

        List<ReorderSectionsRequest> requests = List.of(
                new ReorderSectionsRequest("section-101", 2),
                new ReorderSectionsRequest("section-202", 1)
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.findBySectionId("section-202")).thenReturn(Optional.of(sectionTwo));
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101")).thenReturn(List.of(sectionTwo, section));

        var response = sectionService.reorderSections("resume-101", requests);

        assertEquals(2, response.size());
        verify(sectionRepository).save(section);
        verify(sectionRepository).save(sectionTwo);
    }

    @Test
    void reorderSectionsShouldRejectEmptyRequest() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);

        assertThrows(BadRequestException.class, () -> sectionService.reorderSections("resume-101", List.of()));
    }

    @Test
    void reorderSectionsShouldRejectDuplicateOrders() {
        List<ReorderSectionsRequest> requests = List.of(
                new ReorderSectionsRequest("section-101", 1),
                new ReorderSectionsRequest("section-202", 1)
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);

        assertThrows(BadRequestException.class, () -> sectionService.reorderSections("resume-101", requests));
    }

    @Test
    void reorderSectionsShouldRejectSectionFromDifferentResume() {
        ResumeSection otherResumeSection = new ResumeSection();
        otherResumeSection.setSectionId("section-202");
        otherResumeSection.setResumeId("resume-other");
        otherResumeSection.setSectionType(SectionType.SKILLS);
        otherResumeSection.setTitle("Skills");
        otherResumeSection.setContent("Java");
        otherResumeSection.setDisplayOrder(2);
        otherResumeSection.setIsVisible(true);
        otherResumeSection.setAiGenerated(false);

        List<ReorderSectionsRequest> requests = List.of(
                new ReorderSectionsRequest("section-202", 1)
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-202")).thenReturn(Optional.of(otherResumeSection));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> sectionService.reorderSections("resume-101", requests)
        );

        assertTrue(exception.getMessage().contains("Section does not belong to resume"));
    }

    @Test
    void toggleVisibilityShouldUpdateVisibility() {
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.toggleVisibility("section-101", false);

        assertFalse(response.isVisible());
    }

    @Test
    void deleteAllSectionsShouldValidateResumeAndDeleteByResumeId() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);

        sectionService.deleteAllSections("resume-101");

        verify(sectionRepository).deleteByResumeId("resume-101");
    }

    @Test
    void getSectionsByTypeShouldReturnMatchingType() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findByResumeIdAndSectionType("resume-101", SectionType.SUMMARY)).thenReturn(List.of(section));

        var response = sectionService.getSectionsByType("resume-101", "summary");

        assertEquals(1, response.size());
        assertEquals(SectionType.SUMMARY, response.get(0).sectionType());
    }

    @Test
    void getSectionsByTypeShouldRejectInvalidType() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);

        assertThrows(IllegalArgumentException.class, () -> sectionService.getSectionsByType("resume-101", "invalid"));
    }

    @Test
    void bulkUpdateSectionsShouldCreateNewSectionWhenSectionIdMissing() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(
                        null,
                        "resume-101",
                        "SUMMARY",
                        "Summary",
                        "New summary",
                        1,
                        true,
                        false
                )
        ));

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101")).thenReturn(List.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.bulkUpdateSections(request);

        assertEquals(1, response.size());
        verify(sectionRepository).save(any(ResumeSection.class));
    }

    @Test
    void bulkUpdateSectionsShouldUpdateExistingSection() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(
                        "section-101",
                        "resume-101",
                        "EXPERIENCE",
                        "Experience",
                        "Built APIs",
                        2,
                        false,
                        true
                )
        ));

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101")).thenReturn(List.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.bulkUpdateSections(request);

        assertEquals(1, response.size());
        assertEquals(SectionType.EXPERIENCE, section.getSectionType());
        assertEquals("Experience", section.getTitle());
        assertEquals("Built APIs", section.getContent());
        assertEquals(2, section.getDisplayOrder());
        assertFalse(section.getIsVisible());
        assertTrue(section.getAiGenerated());
    }

    @Test
    void bulkUpdateSectionsShouldRejectEmptySections() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of());

        assertThrows(BadRequestException.class, () -> sectionService.bulkUpdateSections(request));
    }

    @Test
    void bulkUpdateSectionsShouldRejectMismatchedResumeIds() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(null, "resume-101", "SUMMARY", "Summary", "Content", 1, true, false),
                new BulkUpdateSectionsRequest.SectionItem(null, "resume-202", "SKILLS", "Skills", "Java", 2, true, false)
        ));

        assertThrows(BadRequestException.class, () -> sectionService.bulkUpdateSections(request));
    }

    @Test
    void bulkUpdateSectionsShouldRejectExistingSectionFromDifferentResume() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(
                        "section-101",
                        "resume-202",
                        "SUMMARY",
                        "Summary",
                        "Content",
                        1,
                        true,
                        false
                )
        ));

        ResumeSummaryResponse otherResume = new ResumeSummaryResponse(
                "resume-202",
                "user-101",
                "Other Resume",
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

        section.setResumeId("resume-101");

        when(resumeServiceClient.getResumeById("resume-202")).thenReturn(otherResume);
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> sectionService.bulkUpdateSections(request)
        );

        assertTrue(exception.getMessage().contains("Section does not belong to resume"));
    }

    @Test
    void cloneSectionsShouldCopySectionsFromSourceToTarget() {
        ResumeSummaryResponse targetResume = new ResumeSummaryResponse(
                "resume-202",
                "user-101",
                "Target Resume",
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

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(resumeServiceClient.getResumeById("resume-202")).thenReturn(targetResume);
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101")).thenReturn(List.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sectionService.cloneSections("resume-101", "resume-202");

        verify(sectionRepository).save(any(ResumeSection.class));
    }
}
