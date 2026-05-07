package com.resumeai.resume.service;

import com.resumeai.resume.client.AuthServiceClient;
import com.resumeai.resume.client.SectionServiceClient;
import com.resumeai.resume.client.TemplateServiceClient;
import com.resumeai.resume.dto.AuthUserResponse;
import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeAdminStatsResponse;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.TemplateResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.enums.ResumeStatus;
import com.resumeai.resume.exception.BadRequestException;
import com.resumeai.resume.exception.ResourceNotFoundException;
import com.resumeai.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    private final ResumeRepository resumeRepository;
    private final AuthServiceClient authServiceClient;
    private final TemplateServiceClient templateServiceClient;
    private final SectionServiceClient sectionServiceClient;

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             AuthServiceClient authServiceClient,
                             TemplateServiceClient templateServiceClient,
                             SectionServiceClient sectionServiceClient) {
        this.resumeRepository = resumeRepository;
        this.authServiceClient = authServiceClient;
        this.templateServiceClient = templateServiceClient;
        this.sectionServiceClient = sectionServiceClient;
    }

    @Override
    public ResumeResponse createResume(CreateResumeRequest request) {
        log.info("Creating resume for userId={} templateId={} targetJobTitle={}",
                request.userId(), request.templateId(), request.targetJobTitle());
        AuthUserResponse user = authServiceClient.getUserById(request.userId());
        validateUser(user);

        TemplateResponse template = templateServiceClient.validateTemplateAccess(
                request.templateId(),
                request.userId()
        );

        if (template == null || !Boolean.TRUE.equals(template.isActive())) {
            throw new BadRequestException("Selected template is not available");
        }

        long existingCount = resumeRepository.countByUserId(request.userId());
        if ("FREE".equalsIgnoreCase(user.subscriptionPlan()) && existingCount >= 3) {
            throw new BadRequestException("Free users can create up to 3 resumes only");
        }

        Resume resume = new Resume();
        resume.setUserId(request.userId());
        resume.setTitle(request.title());
        resume.setTargetJobTitle(request.targetJobTitle());
        resume.setTemplateId(request.templateId());
        resume.setLanguage(request.language());
        resume.setStatus(ResumeStatus.DRAFT);
        resume.setAtsScore(null);
        resume.setIsPublic(false);
        resume.setViewCount(0);

        Resume saved = resumeRepository.save(resume);
        log.info("Created resume resumeId={} userId={}", saved.getResumeId(), saved.getUserId());

        templateServiceClient.incrementUsage(request.templateId());

        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(String resumeId) {
        log.debug("Fetching resume by id resumeId={}", resumeId);
        return map(getResumeEntity(resumeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByUser(String userId) {
        log.debug("Fetching resumes by userId={}", userId);
        authServiceClient.getUserById(userId);
        return resumeRepository.findByUserId(userId).stream().map(this::map).toList();
    }

    @Override
    public ResumeResponse updateResume(String resumeId, UpdateResumeRequest request) {
        log.info("Updating resume resumeId={} templateId={} status={}",
                resumeId, request.templateId(), request.status());
        Resume resume = getResumeEntity(resumeId);

        if (!request.templateId().equals(resume.getTemplateId())) {
            TemplateResponse template = templateServiceClient.validateTemplateAccess(
                    request.templateId(),
                    resume.getUserId()
            );

            if (template == null || !Boolean.TRUE.equals(template.isActive())) {
                throw new BadRequestException("Selected template is not available");
            }

            templateServiceClient.incrementUsage(request.templateId());
            resume.setTemplateId(request.templateId());
        }

        resume.setTitle(request.title());
        resume.setTargetJobTitle(request.targetJobTitle());

        if (request.language() != null && !request.language().isBlank()) {
            resume.setLanguage(request.language());
        }
        if (request.status() != null) {
            resume.setStatus(request.status());
        }

        return map(resumeRepository.save(resume));
    }

    @Override
    public void deleteResume(String resumeId) {
        log.info("Deleting resume resumeId={}", resumeId);
        Resume resume = getResumeEntity(resumeId);
        sectionServiceClient.deleteAllSections(resumeId);
        resumeRepository.delete(resume);
    }

    @Override
    public ResumeResponse duplicateResume(String resumeId) {
        log.info("Duplicating resume resumeId={}", resumeId);
        Resume original = getResumeEntity(resumeId);
        AuthUserResponse user = authServiceClient.getUserById(original.getUserId());
        validateUser(user);

        TemplateResponse template = templateServiceClient.validateTemplateAccess(
                original.getTemplateId(),
                original.getUserId()
        );

        if (template == null || !Boolean.TRUE.equals(template.isActive())) {
            throw new BadRequestException("Selected template is not available");
        }

        long existingCount = resumeRepository.countByUserId(original.getUserId());
        if ("FREE".equalsIgnoreCase(user.subscriptionPlan()) && existingCount >= 3) {
            throw new BadRequestException("Free users can create up to 3 resumes only");
        }

        Resume copy = new Resume();
        copy.setUserId(original.getUserId());
        copy.setTitle(original.getTitle() + " Copy");
        copy.setTargetJobTitle(original.getTargetJobTitle());
        copy.setTemplateId(original.getTemplateId());
        copy.setAtsScore(original.getAtsScore());
        copy.setStatus(ResumeStatus.DRAFT);
        copy.setLanguage(original.getLanguage());
        copy.setIsPublic(false);
        copy.setViewCount(0);

        Resume savedCopy = resumeRepository.save(copy);

        sectionServiceClient.cloneSections(original.getResumeId(), savedCopy.getResumeId());
        templateServiceClient.incrementUsage(original.getTemplateId());

        return map(savedCopy);
    }

    @Override
    public ResumeResponse updateAtsScore(String resumeId, Integer atsScore) {
        log.info("Updating ATS score resumeId={} atsScore={}", resumeId, atsScore);
        Resume resume = getResumeEntity(resumeId);
        resume.setAtsScore(atsScore);
        return map(resumeRepository.save(resume));
    }

    @Override
    public ResumeResponse publishResume(String resumeId) {
        Resume resume = getResumeEntity(resumeId);
        validateResumeCanBePublished(resumeId);
        resume.setIsPublic(true);
        resume.setStatus(ResumeStatus.COMPLETE);
        log.info("Publishing resume resumeId={}", resumeId);
        return map(resumeRepository.save(resume));
    }

    @Override
    public ResumeResponse unpublishResume(String resumeId) {
        Resume resume = getResumeEntity(resumeId);
        resume.setIsPublic(false);
        resume.setStatus(ResumeStatus.DRAFT);
        log.info("Unpublishing resume resumeId={}", resumeId);
        return map(resumeRepository.save(resume));
    }

    @Override
    public ResumeResponse getPublicResume(String resumeId) {
        Resume resume = getResumeEntity(resumeId);

        if (!Boolean.TRUE.equals(resume.getIsPublic())) {
            throw new ResourceNotFoundException("Public resume not found with id: " + resumeId);
        }

        resume.setViewCount(resume.getViewCount() + 1);
        log.info("Serving public resume resumeId={} updatedViews={}", resumeId, resume.getViewCount());
        return map(resumeRepository.save(resume));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getPublicResumes() {
        log.debug("Fetching public resume gallery");
        return resumeRepository.findTop12ByIsPublicTrueOrderByViewCountDescCreatedAtDesc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> searchPublicResumes(String keyword) {
        log.debug("Searching public resumes keyword={}", keyword);
        if (keyword == null || keyword.isBlank()) {
            return getPublicResumes();
        }
        return resumeRepository.findByIsPublicTrueAndTargetJobTitleContainingIgnoreCaseOrderByViewCountDescCreatedAtDesc(keyword.trim())
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public ResumeResponse incrementViewCount(String resumeId) {
        Resume resume = getResumeEntity(resumeId);
        resume.setViewCount(resume.getViewCount() + 1);
        log.debug("Incrementing view count resumeId={} views={}", resumeId, resume.getViewCount());
        return map(resumeRepository.save(resume));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByTemplate(String templateId) {
        return resumeRepository.findByTemplateId(templateId).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getAllResumesForAdmin() {
        return resumeRepository.findAll().stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeAdminStatsResponse getAdminStats() {
        List<Resume> resumes = resumeRepository.findAll();

        long publicCount = resumes.stream()
                .filter(resume -> Boolean.TRUE.equals(resume.getIsPublic()))
                .count();

        long draftCount = resumes.stream()
                .filter(resume -> resume.getStatus() == ResumeStatus.DRAFT)
                .count();

        long completeCount = resumes.stream()
                .filter(resume -> resume.getStatus() == ResumeStatus.COMPLETE)
                .count();

        long totalViews = resumes.stream()
                .mapToLong(resume -> resume.getViewCount() != null ? resume.getViewCount() : 0)
                .sum();

        return new ResumeAdminStatsResponse(
                resumes.size(),
                publicCount,
                draftCount,
                completeCount,
                totalViews
        );
    }

    private void validateUser(AuthUserResponse user) {
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BadRequestException("User account is inactive");
        }
    }

    private void validateResumeCanBePublished(String resumeId) {
        var sections = sectionServiceClient.getSectionsByResume(resumeId);
        if (sections == null || sections.isEmpty()) {
            throw new BadRequestException("Resume must contain at least one section before publishing");
        }
        boolean hasVisibleContent = sections.stream()
                .anyMatch(section -> Boolean.TRUE.equals(section.isVisible())
                        && section.content() != null
                        && !section.content().isBlank());
        if (!hasVisibleContent) {
            throw new BadRequestException("Resume must contain visible section content before publishing");
        }
    }

    private Resume getResumeEntity(String resumeId) {
        return resumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
    }

    private ResumeResponse map(Resume resume) {
        return new ResumeResponse(
                resume.getResumeId(),
                resume.getUserId(),
                resume.getTitle(),
                resume.getTargetJobTitle(),
                resume.getTemplateId(),
                resume.getAtsScore(),
                resume.getStatus(),
                resume.getLanguage(),
                resume.getIsPublic(),
                resume.getViewCount(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
