package com.resumeai.export.repository;

import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.enums.ExportFormat;
import com.resumeai.export.enums.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExportRepository extends JpaRepository<ExportJob, String> {

    Optional<ExportJob> findByJobId(String jobId);

    Optional<ExportJob> findByDownloadToken(String downloadToken);

    List<ExportJob> findByUserId(String userId);

    List<ExportJob> findByResumeId(String resumeId);

    List<ExportJob> findByStatus(ExportStatus status);

    List<ExportJob> findByFormat(ExportFormat format);

    List<ExportJob> findByExpiresAtBefore(LocalDateTime expiryTime);

    long countByUserIdAndFormatAndRequestedAtBetween(String userId,
                                                     ExportFormat format,
                                                     LocalDateTime start,
                                                     LocalDateTime end);
}
