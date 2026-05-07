package com.resumeai.ai.repository;

import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.enums.RequestStatus;
import com.resumeai.ai.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiRequestRepository extends JpaRepository<AiRequest, String> {

    List<AiRequest> findByUserId(String userId);

    List<AiRequest> findByResumeId(String resumeId);

    Optional<AiRequest> findByRequestId(String requestId);

    List<AiRequest> findByRequestType(RequestType requestType);

    List<AiRequest> findByStatus(RequestStatus status);

    long countByUserIdAndRequestTypeAndCreatedAtBetween(
            String userId,
            RequestType requestType,
            LocalDateTime start,
            LocalDateTime end
    );

    List<AiRequest> findByUserIdOrderByCreatedAtDesc(String userId);
}
