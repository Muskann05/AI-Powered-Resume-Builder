package com.resumeai.section.service;

import com.resumeai.section.client.ResumeServiceClient;
import com.resumeai.section.dto.BulkUpdateSectionsRequest;
import com.resumeai.section.dto.CreateSectionRequest;
import com.resumeai.section.dto.ReorderSectionsRequest;
import com.resumeai.section.dto.ResumeSummaryResponse;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.UpdateSectionRequest;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.enums.SectionType;
import com.resumeai.section.exception.BadRequestException;
import com.resumeai.section.exception.ResourceNotFoundException;
import com.resumeai.section.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceImplTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @InjectMocks
    private SectionServiceImpl sectionService;

    private ResumeSummaryResponse resume;
    private ResumeSummaryResponse targetResume;
    private ResumeSection section;

    @BeforeEach
    void setUp() {
        resume = new ResumeSummaryResponse(
                "resume-101", "user-101", "Backend Resume", "Java Developer",
                "template-101", 80, "DRAFT", "English", false, 0, null, null
        );

        targetResume = new ResumeSummaryResponse(
                "resume-202", "user-101", "Backend Resume Copy", "Java Developer",
                "template-101", 80, "DRAFT", "English", false, 0, null, null
        );

        section = new ResumeSection();
        section.setSectionId("section-101");
        section.setResumeId("resume-101");
        section.setSectionType(SectionType.SUMMARY);
        section.setTitle("Summary");
        section.setContent("Java backend developer");
        section.setDisplayOrder(1);
        section.setIsVisible(true);
        section.setAiGenerated(false);
    }

    @Test
    void addSectionShouldCreateSectionWithRequestedOrder() {
        CreateSectionRequest request = new CreateSectionRequest(
                "resume-101", SectionType.EXPERIENCE, "Experience",
                "Worked on Spring Boot APIs", 2, true, true
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection saved = invocation.getArgument(0);
            saved.setSectionId("section-102");
            return saved;
        });

        SectionResponse response = sectionService.addSection(request);

        assertEquals("section-102", response.sectionId());
        assertEquals("resume-101", response.resumeId());
        assertEquals(SectionType.EXPERIENCE, response.sectionType());
        assertEquals(2, response.displayOrder());
        assertTrue(response.isVisible());
        assertTrue(response.aiGenerated());
    }

    @Test
    void addSectionShouldResolveDisplayOrderAndDefaults() {
        CreateSectionRequest request = new CreateSectionRequest(
                "resume-101", SectionType.SKILLS, "Skills",
                "Java, Spring Boot", null, null, null
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.countByResumeId("resume-101")).thenReturn(2L);
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection saved = invocation.getArgument(0);
            saved.setSectionId("section-103");
            return saved;
        });

        SectionResponse response = sectionService.addSection(request);

        assertEquals(3, response.displayOrder());
        assertTrue(response.isVisible());
        assertFalse(response.aiGenerated());
    }

    @Test
    void addSectionShouldRejectBlankResumeId() {
        CreateSectionRequest request = new CreateSectionRequest(
                "", SectionType.SUMMARY, "Summary", "Content", 1, true, false
        );

        assertThrows(BadRequestException.class, () -> sectionService.addSection(request));
        verify(resumeServiceClient, never()).getResumeById(any());
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void getSectionsByResumeShouldReturnOrderedSections() {
        ResumeSection second = section("section-102", "resume-101", SectionType.SKILLS, "Skills", "Java", 2);

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101"))
                .thenReturn(List.of(section, second));

        List<SectionResponse> responses = sectionService.getSectionsByResume("resume-101");

        assertEquals(2, responses.size());
        assertEquals("section-101", responses.get(0).sectionId());
        assertEquals("section-102", responses.get(1).sectionId());
    }

    @Test
    void getSectionByIdShouldReturnSection() {
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));

        SectionResponse response = sectionService.getSectionById("section-101");

        assertEquals("section-101", response.sectionId());
        assertEquals("Summary", response.title());
    }

    @Test
    void getSectionByIdShouldThrowWhenMissing() {
        when(sectionRepository.findBySectionId("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sectionService.getSectionById("missing"));
    }

    @Test
    void updateSectionShouldUpdateAllFields() {
        UpdateSectionRequest request = new UpdateSectionRequest(
                SectionType.EXPERIENCE, "Experience", "Built APIs", 3, false, true
        );

        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(section)).thenReturn(section);

        SectionResponse response = sectionService.updateSection("section-101", request);

        assertEquals(SectionType.EXPERIENCE, response.sectionType());
        assertEquals("Experience", response.title());
        assertEquals("Built APIs", response.content());
        assertEquals(3, response.displayOrder());
        assertFalse(response.isVisible());
        assertTrue(response.aiGenerated());
    }

    @Test
    void updateSectionShouldKeepOptionalFieldsWhenNull() {
        UpdateSectionRequest request = new UpdateSectionRequest(
                SectionType.SUMMARY, "Summary Updated", "Updated content", null, null, null
        );

        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(section)).thenReturn(section);

        SectionResponse response = sectionService.updateSection("section-101", request);

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
        ResumeSection second = section("section-102", "resume-101", SectionType.SKILLS, "Skills", "Java", 2);

        List<ReorderSectionsRequest> requests = List.of(
                new ReorderSectionsRequest("section-101", 2),
                new ReorderSectionsRequest("section-102", 1)
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.findBySectionId("section-102")).thenReturn(Optional.of(second));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101"))
                .thenReturn(List.of(second, section));

        List<SectionResponse> responses = sectionService.reorderSections("resume-101", requests);

        assertEquals(2, responses.size());
        assertEquals("section-102", responses.get(0).sectionId());
        assertEquals("section-101", responses.get(1).sectionId());
        assertEquals(2, section.getDisplayOrder());
        assertEquals(1, second.getDisplayOrder());
    }

    @Test
    void reorderSectionsShouldRejectEmptyRequest() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);

        assertThrows(BadRequestException.class,
                () -> sectionService.reorderSections("resume-101", List.of()));
    }

    @Test
    void reorderSectionsShouldRejectDuplicateOrders() {
        List<ReorderSectionsRequest> requests = List.of(
                new ReorderSectionsRequest("section-101", 1),
                new ReorderSectionsRequest("section-202", 1)
        );

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));

        assertThrows(BadRequestException.class,
                () -> sectionService.reorderSections("resume-101", requests));

        verify(sectionRepository).findBySectionId("section-101");
        verify(sectionRepository, never()).findBySectionId("section-202");
    }

    @Test
    void reorderSectionsShouldRejectSectionFromDifferentResume() {
        ResumeSection other = section("section-202", "resume-other", SectionType.SKILLS, "Skills", "Java", 2);

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-202")).thenReturn(Optional.of(other));

        assertThrows(BadRequestException.class,
                () -> sectionService.reorderSections("resume-101",
                        List.of(new ReorderSectionsRequest("section-202", 1))));
    }

    @Test
    void toggleVisibilityShouldUpdateVisibility() {
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(section)).thenReturn(section);

        SectionResponse response = sectionService.toggleVisibility("section-101", false);

        assertFalse(response.isVisible());
        verify(sectionRepository).save(section);
    }

    @Test
    void deleteAllSectionsShouldDeleteByResumeId() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);

        sectionService.deleteAllSections("resume-101");

        verify(sectionRepository).deleteByResumeId("resume-101");
    }

    @Test
    void getSectionsByTypeShouldReturnMatchingSections() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findByResumeIdAndSectionType("resume-101", SectionType.SUMMARY))
                .thenReturn(List.of(section));

        List<SectionResponse> responses = sectionService.getSectionsByType("resume-101", "summary");

        assertEquals(1, responses.size());
        assertEquals(SectionType.SUMMARY, responses.get(0).sectionType());
    }

    @Test
    void bulkUpdateSectionsShouldCreateAndUpdateSections() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(
                        null, "resume-101", "SKILLS", "Skills",
                        "Java, Spring Boot", 2, true, false
                ),
                new BulkUpdateSectionsRequest.SectionItem(
                        "section-101", "resume-101", "SUMMARY", "Summary Updated",
                        "Updated content", 1, true, true
                )
        ));

        ResumeSection newSection = section("section-102", "resume-101", SectionType.SKILLS, "Skills", "Java, Spring Boot", 2);

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-101")).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection saved = invocation.getArgument(0);
            if (saved.getSectionId() == null) {
                saved.setSectionId("section-102");
            }
            return saved;
        });
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101"))
                .thenReturn(List.of(section, newSection));

        List<SectionResponse> responses = sectionService.bulkUpdateSections(request);

        assertEquals(2, responses.size());
        assertEquals("Summary Updated", section.getTitle());
        assertEquals("Updated content", section.getContent());
        assertTrue(section.getAiGenerated());
    }

    @Test
    void bulkUpdateSectionsShouldRejectEmptyRequest() {
        assertThrows(BadRequestException.class,
                () -> sectionService.bulkUpdateSections(new BulkUpdateSectionsRequest(List.of())));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void bulkUpdateSectionsShouldRejectMismatchedResumeIds() {
        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(
                        null, "resume-101", "SUMMARY", "Summary", "Content", 1, true, false
                ),
                new BulkUpdateSectionsRequest.SectionItem(
                        null, "resume-202", "SKILLS", "Skills", "Java", 2, true, false
                )
        ));

        assertThrows(BadRequestException.class, () -> sectionService.bulkUpdateSections(request));
        verify(resumeServiceClient, never()).getResumeById(any());
    }

    @Test
    void bulkUpdateSectionsShouldRejectExistingSectionFromDifferentResume() {
        ResumeSection other = section("section-202", "resume-other", SectionType.SKILLS, "Skills", "Java", 2);

        BulkUpdateSectionsRequest request = new BulkUpdateSectionsRequest(List.of(
                new BulkUpdateSectionsRequest.SectionItem(
                        "section-202", "resume-101", "SKILLS", "Skills", "Java", 2, true, false
                )
        ));

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(sectionRepository.findBySectionId("section-202")).thenReturn(Optional.of(other));

        assertThrows(BadRequestException.class, () -> sectionService.bulkUpdateSections(request));
    }

    @Test
    void cloneSectionsShouldCopyAllSourceSections() {
        ResumeSection source = section("section-301", "resume-101", SectionType.PROJECTS, "Projects", "ResumeAI project", 3);

        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(resumeServiceClient.getResumeById("resume-202")).thenReturn(targetResume);
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101"))
                .thenReturn(List.of(section, source));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sectionService.cloneSections("resume-101", "resume-202");

        verify(sectionRepository).findByResumeIdOrderByDisplayOrderAsc("resume-101");
        verify(sectionRepository, times(2)).save(any(ResumeSection.class));
    }

    @Test
    void cloneSectionsShouldHandleNoSourceSections() {
        when(resumeServiceClient.getResumeById("resume-101")).thenReturn(resume);
        when(resumeServiceClient.getResumeById("resume-202")).thenReturn(targetResume);
        when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-101")).thenReturn(List.of());

        sectionService.cloneSections("resume-101", "resume-202");

        verify(sectionRepository, never()).save(any());
    }

    private ResumeSection section(String sectionId,
                                  String resumeId,
                                  SectionType sectionType,
                                  String title,
                                  String content,
                                  Integer displayOrder) {
        ResumeSection resumeSection = new ResumeSection();
        resumeSection.setSectionId(sectionId);
        resumeSection.setResumeId(resumeId);
        resumeSection.setSectionType(sectionType);
        resumeSection.setTitle(title);
        resumeSection.setContent(content);
        resumeSection.setDisplayOrder(displayOrder);
        resumeSection.setIsVisible(true);
        resumeSection.setAiGenerated(false);
        return resumeSection;
    }
}
