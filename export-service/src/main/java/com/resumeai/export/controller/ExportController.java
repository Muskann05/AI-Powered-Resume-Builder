package com.resumeai.export.controller;

import com.resumeai.export.dto.ApiMessageResponse;
import com.resumeai.export.dto.DownloadLinkResponse;
import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;
import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.exception.BadRequestException;
import com.resumeai.export.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/pdf")
    public ResponseEntity<ExportJobResponse> exportPdf(@Valid @RequestBody ExportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                exportService.submitExport(withFormat(request, ExportFormat.PDF))
        );
    }

    @PostMapping("/docx")
    public ResponseEntity<ExportJobResponse> exportDocx(@Valid @RequestBody ExportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                exportService.submitExport(withFormat(request, ExportFormat.DOCX))
        );
    }

    @PostMapping("/json")
    public ResponseEntity<ExportJobResponse> exportJson(@Valid @RequestBody ExportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                exportService.submitExport(withFormat(request, ExportFormat.JSON))
        );
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ExportJobResponse> getJobStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(exportService.getJobStatus(jobId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExportJobResponse>> getExportsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(exportService.getExportsByUser(userId));
    }

    @GetMapping("/{jobId}/download-link")
    public ResponseEntity<DownloadLinkResponse> generateDownloadLink(@PathVariable String jobId,
                                                                     @RequestParam String userId) {
        return ResponseEntity.ok(exportService.generateDownloadLink(jobId, userId));
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String token) {
        String filePath = exportService.getDownloadPathByToken(token);
        File file = new File(filePath);
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiMessageResponse> deleteExport(@PathVariable String jobId) {
        exportService.deleteExport(jobId);
        return ResponseEntity.ok(new ApiMessageResponse("Export deleted successfully"));
    }

    @GetMapping("/stats/all")
    public ResponseEntity<List<ExportJobResponse>> getExportStats() {
        return ResponseEntity.ok(exportService.getExportStats());
    }

    private ExportRequest withFormat(ExportRequest request, ExportFormat expectedFormat) {
        if (request.format() != expectedFormat) {
            throw new BadRequestException("Request format does not match endpoint format");
        }

        return new ExportRequest(
                request.resumeId(),
                request.userId(),
                expectedFormat,
                request.templateId(),
                request.customizations(),
                request.resumeDataJson(),
                request.htmlSnapshot(),
                request.cssSnapshot()
        );
    }
}
