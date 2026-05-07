package com.resumeai.export.service;

import com.resumeai.export.client.AuthServiceClient;
import com.resumeai.export.client.ResumeServiceClient;
import com.resumeai.export.client.SectionServiceClient;
import com.resumeai.export.client.TemplateServiceClient;
import com.resumeai.export.config.RabbitMqConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final ExportRepository exportRepository;
    private final RabbitTemplate rabbitTemplate;
    private final LocalStorageService localStorageService;
    private final AuthServiceClient authServiceClient;
    private final ResumeServiceClient resumeServiceClient;
    private final SectionServiceClient sectionServiceClient;
    private final TemplateServiceClient templateServiceClient;
    private final ExportContentBuilderService exportContentBuilderService;
    private final ExportWorker exportWorker;

    @Value("${export.file-expiry-days}")
    private int expiryDays;

    @Value("${export.processing.mode:sync}")
    private String exportProcessingMode;

    public ExportServiceImpl(ExportRepository exportRepository,
                             RabbitTemplate rabbitTemplate,
                             LocalStorageService localStorageService,
                             AuthServiceClient authServiceClient,
                             ResumeServiceClient resumeServiceClient,
                             SectionServiceClient sectionServiceClient,
                             TemplateServiceClient templateServiceClient,
                             ExportContentBuilderService exportContentBuilderService,
                             ExportWorker exportWorker) {
        this.exportRepository = exportRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.localStorageService = localStorageService;
        this.authServiceClient = authServiceClient;
        this.resumeServiceClient = resumeServiceClient;
        this.sectionServiceClient = sectionServiceClient;
        this.templateServiceClient = templateServiceClient;
        this.exportContentBuilderService = exportContentBuilderService;
        this.exportWorker = exportWorker;
    }

    @Override
    public ExportJobResponse submitExport(ExportRequest request) {
        log.info("Submitting export resumeId={} userId={} format={} templateId={}",
                request.resumeId(), request.userId(), request.format(), request.templateId());
        AuthUserResponse user = getValidatedUser(request.userId());
        boolean premiumUser = isPremiumUser(user);

        ResumeSummaryResponse resume = resumeServiceClient.getResumeById(request.resumeId());
        validateResumeOwnership(request.userId(), resume);

        TemplateResponse template = templateServiceClient.validateTemplateAccess(
                request.templateId(),
                request.userId()
        );

        List<SectionResponse> sections = sectionServiceClient.getSectionsByResume(request.resumeId());

        validateFormatAccess(request.format(), premiumUser);
        enforceFreePdfQuota(request.userId(), request.format(), premiumUser);

        String resumeDataJson = hasText(request.resumeDataJson())
                ? request.resumeDataJson()
                : exportContentBuilderService.buildResumeJson(resume, sections, request.customizations());

        String htmlSnapshot = hasText(request.htmlSnapshot())
                ? request.htmlSnapshot()
                : exportContentBuilderService.buildHtml(resume, sections, template);

        String cssSnapshot = hasText(request.cssSnapshot())
                ? request.cssSnapshot()
                : exportContentBuilderService.buildCss(template);

        ExportJob job = new ExportJob();
        job.setResumeId(request.resumeId());
        job.setUserId(request.userId());
        job.setFormat(request.format());
        job.setStatus(ExportStatus.QUEUED);
        job.setTemplateId(request.templateId());
        job.setCustomizations(request.customizations());
        job.setResumeDataJson(resumeDataJson);
        job.setHtmlSnapshot(htmlSnapshot);
        job.setCssSnapshot(cssSnapshot);
        job.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));

        job = exportRepository.save(job);
        log.info("Queued export job jobId={} mode={}", job.getJobId(), exportProcessingMode);

        publishExportMessageAfterCommit(job.getJobId());

        return map(job);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getJobStatus(String jobId) {
        log.debug("Fetching export job status jobId={}", jobId);
        return map(getJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportJobResponse> getExportsByUser(String userId) {
        log.debug("Fetching exports by userId={}", userId);
        getValidatedUser(userId);
        return exportRepository.findByUserId(userId).stream().map(this::map).toList();
    }

    @Override
    public DownloadLinkResponse generateDownloadLink(String jobId, String userId) {
        log.info("Generating download link jobId={} userId={}", jobId, userId);
        getValidatedUser(userId);

        ExportJob job = getJob(jobId);
        if (!job.getUserId().equals(userId)) {
            throw new BadRequestException("Export job does not belong to the user");
        }
        if (job.getStatus() != ExportStatus.COMPLETED || job.getFileUrl() == null) {
            throw new BadRequestException("File is not ready for download");
        }

        if (job.getDownloadToken() == null
                || job.getDownloadTokenExpiresAt() == null
                || job.getDownloadTokenExpiresAt().isBefore(LocalDateTime.now())) {
            job.setDownloadToken(UUID.randomUUID().toString());
            job.setDownloadTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
            job = exportRepository.save(job);
        }

        return new DownloadLinkResponse(
                job.getJobId(),
                "/exports/download/" + job.getDownloadToken(),
                job.getDownloadTokenExpiresAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String getDownloadPathByToken(String token) {
        log.debug("Resolving download path by token");
        ExportJob job = exportRepository.findByDownloadToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid download token"));

        if (job.getDownloadTokenExpiresAt() == null || job.getDownloadTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Download token has expired");
        }

        if (job.getStatus() != ExportStatus.COMPLETED || job.getFileUrl() == null) {
            throw new BadRequestException("File is not ready for download");
        }

        return job.getFileUrl();
    }

    @Override
    public void deleteExport(String jobId) {
        log.info("Deleting export job jobId={}", jobId);
        ExportJob job = getJob(jobId);
        if (job.getFileUrl() != null) {
            localStorageService.deleteFile(job.getFileUrl());
        }
        exportRepository.delete(job);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredExports() {
        List<ExportJob> expiredJobs = exportRepository.findByExpiresAtBefore(LocalDateTime.now());
        if (!expiredJobs.isEmpty()) {
            log.info("Cleaning up {} expired export jobs", expiredJobs.size());
        }
        for (ExportJob job : expiredJobs) {
            if (job.getFileUrl() != null) {
                localStorageService.deleteFile(job.getFileUrl());
            }
            exportRepository.delete(job);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportJobResponse> getExportStats() {
        return exportRepository.findAll().stream().map(this::map).toList();
    }

    private void publishExportMessageAfterCommit(String jobId) {
        Runnable publishAction = () -> {
            if ("sync".equalsIgnoreCase(exportProcessingMode)) {
                log.info("Processing export synchronously jobId={}", jobId);
                exportWorker.processExportJob(jobId);
                return;
            }

            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXPORT_EXCHANGE,
                        RabbitMqConfig.EXPORT_ROUTING_KEY,
                        new ExportMessage(jobId)
                );
                log.info("Published export message to queue jobId={}", jobId);
            } catch (Exception ex) {
                log.warn("Rabbit publish failed for jobId={}, falling back to sync processing. reason={}",
                        jobId, ex.getMessage());
                exportWorker.processExportJob(jobId);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishAction.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishAction.run();
            }
        });
    }

    private AuthUserResponse getValidatedUser(String userId) {
        log.debug("Validating export user userId={}", userId);
        AuthUserResponse user = authServiceClient.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("User not found");
        }
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BadRequestException("User account is inactive");
        }
        return user;
    }

    private boolean isPremiumUser(AuthUserResponse user) {
        return "PREMIUM".equalsIgnoreCase(user.subscriptionPlan());
    }

    private void validateResumeOwnership(String userId, ResumeSummaryResponse resume) {
        if (resume == null || resume.userId() == null || !userId.equals(resume.userId())) {
            throw new BadRequestException("Resume does not belong to the user");
        }
    }

    private void validateFormatAccess(ExportFormat format, boolean premiumUser) {
        if (premiumUser) {
            return;
        }
        if (format != ExportFormat.PDF) {
            throw new BadRequestException("DOCX and JSON export are premium only");
        }
    }

    private void enforceFreePdfQuota(String userId, ExportFormat format, boolean premiumUser) {
        if (premiumUser || format != ExportFormat.PDF) {
            return;
        }

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long pdfCount = exportRepository.countByUserIdAndFormatAndRequestedAtBetween(
                userId, ExportFormat.PDF, start, end
        );

        if (pdfCount >= 10) {
            throw new BadRequestException("Free users can export only 10 PDFs per day");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ExportJob getJob(String jobId) {
        return exportRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Export job not found with id: " + jobId));
    }

    private ExportJobResponse map(ExportJob job) {
        return new ExportJobResponse(
                job.getJobId(),
                job.getResumeId(),
                job.getUserId(),
                job.getFormat(),
                job.getStatus(),
                null,
                job.getFileSizeKb(),
                job.getRequestedAt(),
                job.getCompletedAt(),
                job.getExpiresAt(),
                job.getTemplateId(),
                job.getCustomizations()
        );
    }
}
