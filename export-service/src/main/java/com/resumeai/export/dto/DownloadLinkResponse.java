package com.resumeai.export.dto;

import java.time.LocalDateTime;

public record DownloadLinkResponse(
        String jobId,
        String downloadUrl,
        LocalDateTime expiresAt
) {
}
