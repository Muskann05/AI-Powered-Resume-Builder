package com.resumeai.section.service;

import com.resumeai.section.dto.BulkUpdateSectionsRequest;
import com.resumeai.section.dto.CreateSectionRequest;
import com.resumeai.section.dto.ReorderSectionsRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.UpdateSectionRequest;

import java.util.List;

public interface SectionService {

    SectionResponse addSection(CreateSectionRequest request);

    List<SectionResponse> getSectionsByResume(String resumeId);

    SectionResponse getSectionById(String sectionId);

    SectionResponse updateSection(String sectionId, UpdateSectionRequest request);

    void deleteSection(String sectionId);

    List<SectionResponse> reorderSections(String resumeId, List<ReorderSectionsRequest> requests);

    SectionResponse toggleVisibility(String sectionId, Boolean isVisible);

    void deleteAllSections(String resumeId);

    List<SectionResponse> getSectionsByType(String resumeId, String sectionType);

    List<SectionResponse> bulkUpdateSections(BulkUpdateSectionsRequest request);

    void cloneSections(String sourceResumeId, String targetResumeId);
}
