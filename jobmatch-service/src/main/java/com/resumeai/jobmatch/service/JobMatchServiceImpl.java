package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.client.AiServiceClient;
import com.resumeai.jobmatch.client.AuthServiceClient;
import com.resumeai.jobmatch.client.LinkedInJobClient;
import com.resumeai.jobmatch.client.NotificationServiceClient;
import com.resumeai.jobmatch.client.ResumeServiceClient;
import com.resumeai.jobmatch.client.SectionServiceClient;
import com.resumeai.jobmatch.dto.AiJobFitRequest;
import com.resumeai.jobmatch.dto.AiJobFitResponse;
import com.resumeai.jobmatch.dto.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.JobMatchResponse;
import com.resumeai.jobmatch.dto.JobSearchRequest;
import com.resumeai.jobmatch.dto.LinkedInJobResponse;
import com.resumeai.jobmatch.dto.NotificationRequest;
import com.resumeai.jobmatch.dto.ResumeResponse;
import com.resumeai.jobmatch.dto.SectionResponse;
import com.resumeai.jobmatch.dto.TailoringRecommendationsResponse;
import com.resumeai.jobmatch.dto.UserResponse;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.enums.JobSource;
import com.resumeai.jobmatch.exception.BadRequestException;
import com.resumeai.jobmatch.exception.ResourceNotFoundException;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class JobMatchServiceImpl implements JobMatchService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchServiceImpl.class);

    private final JobMatchRepository jobMatchRepository;
    private final AuthServiceClient authServiceClient;
    private final ResumeServiceClient resumeServiceClient;
    private final SectionServiceClient sectionServiceClient;
    private final AiServiceClient aiServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final LinkedInJobClient linkedInJobClient;
    private final LinkedInJobFallbackService linkedInJobFallbackService;

    @Value("${linkedin.api.key}")
    private String linkedinApiKey;

    @Value("${linkedin.api.default-limit}")
    private Integer linkedinDefaultLimit;

    @Value("${linkedin.api.provider:dummy}")
    private String linkedinApiProvider;

    @Value("${linkedin.api.fallback-enabled:true}")
    private boolean linkedinFallbackEnabled;

    @Value("${jobmatch.notification.threshold}")
    private Integer notificationThreshold;

    public JobMatchServiceImpl(JobMatchRepository jobMatchRepository,
                               AuthServiceClient authServiceClient,
                               ResumeServiceClient resumeServiceClient,
                               SectionServiceClient sectionServiceClient,
                               AiServiceClient aiServiceClient,
                               NotificationServiceClient notificationServiceClient,
                               LinkedInJobClient linkedInJobClient,
                               LinkedInJobFallbackService linkedInJobFallbackService) {
        this.jobMatchRepository = jobMatchRepository;
        this.authServiceClient = authServiceClient;
        this.resumeServiceClient = resumeServiceClient;
        this.sectionServiceClient = sectionServiceClient;
        this.aiServiceClient = aiServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.linkedInJobClient = linkedInJobClient;
        this.linkedInJobFallbackService = linkedInJobFallbackService;
    }

    @Override
    public JobMatchResponse analyzeJobFit(AnalyzeJobFitRequest request) {
        log.info("Analyzing manual job fit for resumeId={} userId={} jobTitle={}",
                request.resumeId(), request.userId(), request.jobTitle());
        UserResponse user = validatePremiumUser(request.userId());
        ResumeResponse resume = validateResumeOwnership(request.resumeId(), request.userId());
        List<SectionResponse> sections = sectionServiceClient.getSectionsByResume(request.resumeId());

        AiJobFitResponse analysis = aiServiceClient.analyzeJobFit(new AiJobFitRequest(
                user.userId(),
                resume.resumeId(),
                resume.title(),
                resume.targetJobTitle(),
                request.jobTitle(),
                request.jobDescription(),
                sections
        ));

        JobMatch jobMatch = new JobMatch();
        jobMatch.setResumeId(request.resumeId());
        jobMatch.setUserId(request.userId());
        jobMatch.setJobTitle(request.jobTitle());
        jobMatch.setJobDescription(request.jobDescription());
        jobMatch.setMatchScore(resolveScore(analysis));
        jobMatch.setMissingSkills(analysis != null ? analysis.missingSkills() : null);
        jobMatch.setRecommendations(analysis != null ? analysis.recommendations() : null);
        jobMatch.setSource(JobSource.MANUAL);

        JobMatch saved = jobMatchRepository.save(jobMatch);
        log.info("Stored manual job match matchId={} score={} source={}",
                saved.getMatchId(), saved.getMatchScore(), saved.getSource());
        sendNotificationIfQualified(saved);
        return map(saved);
    }

    @Override
    public List<JobMatchResponse> fetchLinkedInJobs(JobSearchRequest request) {
        return fetchProviderJobs(request, JobSource.LINKEDIN);
    }

    private List<JobMatchResponse> fetchProviderJobs(JobSearchRequest request, JobSource source) {
        log.info("Fetching LinkedIn jobs for resumeId={} userId={} title={} location={}",
                request.resumeId(), request.userId(), request.title(), request.location());
        validatePremiumUser(request.userId());
        ResumeResponse resume = validateResumeOwnership(request.resumeId(), request.userId());
        List<SectionResponse> sections = sectionServiceClient.getSectionsByResume(request.resumeId());
        Integer limit = request.limit() != null ? request.limit() : linkedinDefaultLimit;

        List<LinkedInJobResponse> jobs = fetchJobsFromConfiguredProvider(request.title(), request.location(), limit);
        log.info("Resolved {} {} job candidates for title={} location={}",
                jobs.size(), source, request.title(), request.location());

        return jobs.stream().map(job -> {
            AiJobFitResponse analysis = aiServiceClient.analyzeJobFit(new AiJobFitRequest(
                    request.userId(),
                    resume.resumeId(),
                    resume.title(),
                    resume.targetJobTitle(),
                    job.title(),
                    job.description(),
                    sections
            ));

            JobMatch entity = new JobMatch();
            entity.setResumeId(request.resumeId());
            entity.setUserId(request.userId());
            entity.setJobTitle(job.title());
            entity.setCompanyName(job.companyName());
            entity.setLocation(job.location());
            entity.setExternalJobId(job.externalJobId());
            entity.setApplyUrl(job.applyUrl());
            entity.setJobDescription(job.description());
            entity.setMatchScore(resolveScore(analysis));
            entity.setMissingSkills(analysis != null ? analysis.missingSkills() : null);
            entity.setRecommendations(analysis != null ? analysis.recommendations() : null);
            entity.setSource(source);

            JobMatch saved = jobMatchRepository.save(entity);
            log.info("Stored provider job match matchId={} externalJobId={} score={} source={}",
                    saved.getMatchId(), saved.getExternalJobId(), saved.getMatchScore(), source);
            sendNotificationIfQualified(saved);
            return map(saved);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMatchesByResume(String resumeId) {
        return jobMatchRepository.findByResumeIdOrderByMatchedAtDesc(resumeId).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMatchesByUser(String userId) {
        return jobMatchRepository.findByUserIdOrderByMatchedAtDesc(userId).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getTopMatches(String userId) {
        return jobMatchRepository.findTop10ByUserIdOrderByMatchScoreDescMatchedAtDesc(userId).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobMatchResponse getMatchById(String matchId) {
        return map(getJobMatchEntity(matchId));
    }

    @Override
    public JobMatchResponse bookmarkMatch(String matchId, Boolean bookmarked) {
        JobMatch match = getJobMatchEntity(matchId);
        match.setIsBookmarked(bookmarked);
        log.info("Updating bookmark state for matchId={} bookmarked={}", matchId, bookmarked);
        return map(jobMatchRepository.save(match));
    }

    @Override
    @Transactional(readOnly = true)
    public TailoringRecommendationsResponse getRecommendations(String matchId) {
        JobMatch match = getJobMatchEntity(matchId);
        return new TailoringRecommendationsResponse(match.getMatchId(), match.getRecommendations());
    }

    @Override
    public void deleteMatch(String matchId) {
        log.info("Deleting job match matchId={}", matchId);
        jobMatchRepository.delete(getJobMatchEntity(matchId));
    }

    private UserResponse validatePremiumUser(String userId) {
        log.debug("Validating premium access for userId={}", userId);
        UserResponse user = authServiceClient.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BadRequestException("User account is inactive");
        }
        if (!"PREMIUM".equalsIgnoreCase(user.subscriptionPlan())) {
            throw new BadRequestException("Job match features are allowed only for premium users");
        }
        return user;
    }

    private ResumeResponse validateResumeOwnership(String resumeId, String userId) {
        log.debug("Validating resume ownership for resumeId={} userId={}", resumeId, userId);
        ResumeResponse resume = resumeServiceClient.getResumeById(resumeId);
        if (resume == null) {
            throw new BadRequestException("Resume not found");
        }
        if (!userId.equals(resume.userId())) {
            throw new BadRequestException("Resume does not belong to the user");
        }
        return resume;
    }

    private Integer resolveScore(AiJobFitResponse analysis) {
        if (analysis == null || analysis.matchScore() == null) {
            log.error("AI job fit response missing match score");
            log.warn("Falling back to heuristic match score because AI job fit response was incomplete");
            return 60;
        }
        return analysis.matchScore();
    }

    private List<LinkedInJobResponse> fetchJobsFromConfiguredProvider(String title, String location, Integer limit) {
        if ("dummy".equalsIgnoreCase(linkedinApiProvider)) {
            log.info("Using dummy LinkedIn provider for title={} location={} limit={}", title, location, limit);
            return linkedInJobFallbackService.generateJobs(title, location, limit);
        }

        try {
            if (linkedinApiKey == null || linkedinApiKey.isBlank() || "replace-with-linkedin-key".equals(linkedinApiKey)) {
                throw new IllegalStateException("LinkedIn API key is not configured");
            }

            List<LinkedInJobResponse> jobs = linkedInJobClient.searchJobs(title, location, limit, linkedinApiKey);
            if (jobs == null || jobs.isEmpty()) {
                log.warn("Remote LinkedIn provider returned no jobs for title={} location={}", title, location);
                if (linkedinFallbackEnabled) {
                    return linkedInJobFallbackService.generateJobs(title, location, limit);
                }
                throw new BadRequestException("LinkedIn provider returned no jobs for the current search");
            }
            return jobs;
        } catch (Exception ex) {
            log.warn("Remote LinkedIn provider failed for title={} location={} reason={}",
                    title, location, ex.getMessage());
            if (linkedinFallbackEnabled) {
                log.info("Switching to dummy LinkedIn fallback for title={} location={}", title, location);
                return linkedInJobFallbackService.generateJobs(title, location, limit);
            }
            throw new BadRequestException("LinkedIn job provider is unavailable right now");
        }
    }

    private void sendNotificationIfQualified(JobMatch jobMatch) {
        if (jobMatch.getMatchScore() < notificationThreshold) {
            log.debug("Skipping job match notification for matchId={} score={} threshold={}",
                    jobMatch.getMatchId(), jobMatch.getMatchScore(), notificationThreshold);
            return;
        }

        try {
            notificationServiceClient.sendNotification(new NotificationRequest(
                    jobMatch.getUserId(),
                    "JOB_MATCH",
                    "High match job found",
                    "A " + jobMatch.getMatchScore() + "% match is available for " + jobMatch.getJobTitle(),
                    jobMatch.getMatchId(),
                    "/job-matches/" + jobMatch.getMatchId()
            ));
            log.info("Sent job match notification for matchId={} userId={}",
                    jobMatch.getMatchId(), jobMatch.getUserId());
        } catch (Exception ex) {
            log.warn("Failed to send job match notification for matchId={} reason={}",
                    jobMatch.getMatchId(), ex.getMessage());
        }
    }

    private JobMatch getJobMatchEntity(String matchId) {
        return jobMatchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Job match not found with id: " + matchId));
    }

    private JobMatchResponse map(JobMatch jobMatch) {
        return new JobMatchResponse(
                jobMatch.getMatchId(),
                jobMatch.getResumeId(),
                jobMatch.getUserId(),
                jobMatch.getJobTitle(),
                jobMatch.getCompanyName(),
                jobMatch.getLocation(),
                jobMatch.getExternalJobId(),
                jobMatch.getApplyUrl(),
                jobMatch.getJobDescription(),
                jobMatch.getMatchScore(),
                jobMatch.getMissingSkills(),
                jobMatch.getRecommendations(),
                jobMatch.getSource(),
                jobMatch.getIsBookmarked(),
                jobMatch.getMatchedAt()
        );
    }
}
