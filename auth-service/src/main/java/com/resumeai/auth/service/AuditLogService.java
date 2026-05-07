package com.resumeai.auth.service;

import com.resumeai.auth.dto.AuditLogResponse;
import com.resumeai.auth.entity.AuditLog;
import com.resumeai.auth.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String actor, String action, String targetType, String targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor != null && !actor.isBlank() ? actor : "SYSTEM");
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> latest(String actor, String action) {
        if (actor != null && !actor.isBlank()) {
            return auditLogRepository.findByActorOrderByCreatedAtDesc(actor).stream().map(this::map).toList();
        }
        if (action != null && !action.isBlank()) {
            return auditLogRepository.findByActionOrderByCreatedAtDesc(action).stream().map(this::map).toList();
        }
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc().stream().map(this::map).toList();
    }

    private AuditLogResponse map(AuditLog log) {
        return new AuditLogResponse(
                log.getAuditId(),
                log.getActor(),
                log.getAction(),
                log.getTargetId(),
                log.getTargetType(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}