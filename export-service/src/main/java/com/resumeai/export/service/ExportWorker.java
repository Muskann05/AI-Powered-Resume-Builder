package com.resumeai.export.service;

import com.resumeai.export.config.RabbitMqConfig;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.enums.ExportStatus;
import com.resumeai.export.messaging.ExportMessage;
import com.resumeai.export.messaging.ExportReadyMessage;
import com.resumeai.export.repository.ExportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;

@Service
public class ExportWorker {

    private static final Logger log = LoggerFactory.getLogger(ExportWorker.class);

    private final ExportRepository exportRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final DocxGeneratorService docxGeneratorService;
    private final JsonGeneratorService jsonGeneratorService;
    private final LocalStorageService localStorageService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${export.local-temp-dir}")
    private String localTempDir;

    @Value("${export.file-expiry-days}")
    private int expiryDays;

    public ExportWorker(ExportRepository exportRepository,
                        PdfGeneratorService pdfGeneratorService,
                        DocxGeneratorService docxGeneratorService,
                        JsonGeneratorService jsonGeneratorService,
                        LocalStorageService localStorageService,
                        RabbitTemplate rabbitTemplate) {
        this.exportRepository = exportRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.docxGeneratorService = docxGeneratorService;
        this.jsonGeneratorService = jsonGeneratorService;
        this.localStorageService = localStorageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.EXPORT_QUEUE)
    @Transactional
    public void processExport(ExportMessage message) {
        processExportJob(message.getJobId());
    }

    @Transactional
    public void processExportJob(String jobId) {
        log.info("Starting export processing jobId={}", jobId);
        ExportJob job = exportRepository.findByJobId(jobId).orElseThrow();
        job.setStatus(ExportStatus.PROCESSING);
        exportRepository.save(job);

        try {
            File dir = new File(localTempDir);
            if (!dir.exists()) dir.mkdirs();

            String ext = job.getFormat().name().toLowerCase();
            File targetFile = new File(dir, job.getJobId() + "." + ext);

            if (job.getFormat() == ExportFormat.PDF) {
                String fullHtml = "<style>" + job.getCssSnapshot() + "</style>" + job.getHtmlSnapshot();
                pdfGeneratorService.generatePdf(fullHtml, targetFile);
            } else if (job.getFormat() == ExportFormat.DOCX) {
                docxGeneratorService.generateDocx(job.getResumeDataJson(), targetFile);
            } else if (job.getFormat() == ExportFormat.JSON) {
                jsonGeneratorService.generateJson(job.getResumeDataJson(), targetFile);
            }

            String storedPath = localStorageService.storeFile(targetFile, targetFile.getName());

            job.setFileUrl(storedPath);
            job.setFileSizeKb((int) (Files.size(targetFile.toPath()) / 1024));
            job.setStatus(ExportStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));

            exportRepository.save(job);
            log.info("Completed export job jobId={} filePath={} sizeKb={}",
                    job.getJobId(), job.getFileUrl(), job.getFileSizeKb());

            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXPORT_EXCHANGE,
                        RabbitMqConfig.EXPORT_READY_ROUTING_KEY,
                        new ExportReadyMessage(job.getJobId(), job.getUserId(), job.getResumeId(), job.getFileUrl())
                );
                log.info("Published export ready event jobId={}", job.getJobId());
            } catch (Exception ex) {
                log.warn("Failed to publish export ready event jobId={} reason={}",
                        job.getJobId(), ex.getMessage());
            }

            Files.deleteIfExists(targetFile.toPath());
        } catch (Exception ex) {
            job.setStatus(ExportStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            exportRepository.save(job);
            log.error("Export processing failed jobId={} reason={}", job.getJobId(), ex.getMessage());
        }
    }
}
