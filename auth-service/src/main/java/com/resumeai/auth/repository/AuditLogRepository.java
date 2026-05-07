package com.resumeai.auth.repository;

import com.resumeai.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findTop200ByOrderByCreatedAtDesc();
    List<AuditLog> findByActorOrderByCreatedAtDesc(String actor);
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}