package com.resumeai.section.service;

import com.resumeai.section.client.ResumeServiceClient;
import com.resumeai.section.dto.BulkUpdateSectionsRequest;
import com.resumeai.section.dto.CreateSectionRequest;
import com.resumeai.section.dto.ReorderSectionsRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.UpdateSectionRequest;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.enums.SectionType;
import com.resumeai.section.exception.BadRequestException;
import com.resumeai.section.exception.ResourceNotFoundException;
import com.resumeai.section.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final ResumeServiceClient resumeServiceClient;

    public SectionServiceImpl(SectionRepository sectionRepository,
                              ResumeServiceClient resumeServiceClient) {
        this.sectionRepository = sectionRepository;
        this.resumeServiceClient = resumeServiceClient;
    }

    @Override
    public SectionResponse addSection(CreateSectionRequest request) {
        validateResume(request.resumeId());

        ResumeSection section = new ResumeSection();
        section.setResumeId(request.resumeId());
        section.setSectionType(request.sectionType());
        section.setTitle(request.title());
        section.setContent(request.content());
        section.setDisplayOrder(resolveDisplayOrder(request.resumeId(), request.displayOrder()));
        section.setIsVisible(request.isVisible() != null ? request.isVisible() : true);
        section.setAiGenerated(request.aiGenerated() != null ? request.aiGenerated() : false);

        return map(sectionRepository.save(section));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByResume(String resumeId) {
        validateResume(resumeId);
        return sectionRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SectionResponse getSectionById(String sectionId) {
        return map(getSectionEntity(sectionId));
    }

    @Override
    public SectionResponse updateSection(String sectionId, UpdateSectionRequest request) {
        ResumeSection section = getSectionEntity(sectionId);
        section.setSectionType(request.sectionType());
        section.setTitle(request.title());
        section.setContent(request.content());

        if (request.displayOrder() != null) {
            section.setDisplayOrder(request.displayOrder());
        }
        if (request.isVisible() != null) {
            section.setIsVisible(request.isVisible());
        }
        if (request.aiGenerated() != null) {
            section.setAiGenerated(request.aiGenerated());
        }

        return map(sectionRepository.save(section));
    }

    @Override
    public void deleteSection(String sectionId) {
        ResumeSection section = getSectionEntity(sectionId);
        sectionRepository.delete(section);
    }

    @Override
    public List<SectionResponse> reorderSections(String resumeId, List<ReorderSectionsRequest> requests) {
        validateResume(resumeId);

        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one section is required for reorder");
        }

        Set<Integer> usedOrders = new HashSet<>();
        for (ReorderSectionsRequest request : requests) {
            if (!usedOrders.add(request.displayOrder())) {
                throw new BadRequestException("Duplicate display order values are not allowed");
            }

            ResumeSection section = getSectionEntity(request.sectionId());
            if (!section.getResumeId().equals(resumeId)) {
                throw new BadRequestException("Section does not belong to resume: " + resumeId);
            }

            section.setDisplayOrder(request.displayOrder());
            sectionRepository.save(section);
        }

        return getSectionsByResume(resumeId);
    }

    @Override
    public SectionResponse toggleVisibility(String sectionId, Boolean isVisible) {
        ResumeSection section = getSectionEntity(sectionId);
        section.setIsVisible(isVisible);
        return map(sectionRepository.save(section));
    }

    @Override
    public void deleteAllSections(String resumeId) {
        validateResume(resumeId);
        sectionRepository.deleteByResumeId(resumeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByType(String resumeId, String sectionType) {
        validateResume(resumeId);
        SectionType type = SectionType.valueOf(sectionType.toUpperCase());
        return sectionRepository.findByResumeIdAndSectionType(resumeId, type).stream().map(this::map).toList();
    }

    @Override
    public List<SectionResponse> bulkUpdateSections(BulkUpdateSectionsRequest request) {
        validateBulkRequest(request);
        String resumeId = request.sections().get(0).resumeId();
        validateResume(resumeId);

        for (BulkUpdateSectionsRequest.SectionItem item : request.sections()) {
            if (item.sectionId() == null || item.sectionId().isBlank()) {
                ResumeSection section = new ResumeSection();
                section.setResumeId(item.resumeId());
                section.setSectionType(SectionType.valueOf(item.sectionType().toUpperCase()));
                section.setTitle(item.title());
                section.setContent(item.content());
                section.setDisplayOrder(item.displayOrder() != null ? item.displayOrder() : resolveDisplayOrder(item.resumeId(), null));
                section.setIsVisible(item.isVisible() != null ? item.isVisible() : true);
                section.setAiGenerated(item.aiGenerated() != null ? item.aiGenerated() : false);
                sectionRepository.save(section);
            } else {
                ResumeSection section = getSectionEntity(item.sectionId());

                if (!section.getResumeId().equals(resumeId)) {
                    throw new BadRequestException("Section does not belong to resume: " + resumeId);
                }

                section.setResumeId(item.resumeId());
                section.setSectionType(SectionType.valueOf(item.sectionType().toUpperCase()));
                section.setTitle(item.title());
                section.setContent(item.content());
                section.setDisplayOrder(item.displayOrder() != null ? item.displayOrder() : section.getDisplayOrder());
                section.setIsVisible(item.isVisible() != null ? item.isVisible() : section.getIsVisible());
                section.setAiGenerated(item.aiGenerated() != null ? item.aiGenerated() : section.getAiGenerated());
                sectionRepository.save(section);
            }
        }

        return getSectionsByResume(resumeId);
    }

    @Override
    public void cloneSections(String sourceResumeId, String targetResumeId) {
        validateResume(sourceResumeId);
        validateResume(targetResumeId);

        List<ResumeSection> sourceSections = sectionRepository.findByResumeIdOrderByDisplayOrderAsc(sourceResumeId);

        for (ResumeSection source : sourceSections) {
            ResumeSection clone = new ResumeSection();
            clone.setResumeId(targetResumeId);
            clone.setSectionType(source.getSectionType());
            clone.setTitle(source.getTitle());
            clone.setContent(source.getContent());
            clone.setDisplayOrder(source.getDisplayOrder());
            clone.setIsVisible(source.getIsVisible());
            clone.setAiGenerated(source.getAiGenerated());
            sectionRepository.save(clone);
        }
    }

    private void validateResume(String resumeId) {
        if (resumeId == null || resumeId.isBlank()) {
            throw new BadRequestException("Resume id is required");
        }
        resumeServiceClient.getResumeById(resumeId);
    }

    private void validateBulkRequest(BulkUpdateSectionsRequest request) {
        if (request.sections() == null || request.sections().isEmpty()) {
            throw new BadRequestException("At least one section is required");
        }

        String resumeId = request.sections().get(0).resumeId();
        if (resumeId == null || resumeId.isBlank()) {
            throw new BadRequestException("Resume id is required for bulk update");
        }

        boolean mismatchedResume = request.sections().stream()
                .anyMatch(item -> item.resumeId() == null || !resumeId.equals(item.resumeId()));
        if (mismatchedResume) {
            throw new BadRequestException("All sections in bulk update must belong to the same resume");
        }
    }

    private ResumeSection getSectionEntity(String sectionId) {
        return sectionRepository.findBySectionId(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));
    }

    private Integer resolveDisplayOrder(String resumeId, Integer requestedOrder) {
        if (requestedOrder != null) {
            return requestedOrder;
        }
        return (int) sectionRepository.countByResumeId(resumeId) + 1;
    }

    private SectionResponse map(ResumeSection section) {
        return new SectionResponse(
                section.getSectionId(),
                section.getResumeId(),
                section.getSectionType(),
                section.getTitle(),
                section.getContent(),
                section.getDisplayOrder(),
                section.getIsVisible(),
                section.getAiGenerated(),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }
}
