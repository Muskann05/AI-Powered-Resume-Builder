package com.resumeai.export.service;

import com.resumeai.export.dto.DownloadLinkResponse;
import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;

import java.util.List;

public interface ExportService {

    ExportJobResponse submitExport(ExportRequest request);

    ExportJobResponse getJobStatus(String jobId);

    List<ExportJobResponse> getExportsByUser(String userId);

    DownloadLinkResponse generateDownloadLink(String jobId, String userId);

    String getDownloadPathByToken(String token);

    void deleteExport(String jobId);

    void cleanupExpiredExports();

    List<ExportJobResponse> getExportStats();
}
