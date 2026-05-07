package com.resumeai.auth.controller;

import com.resumeai.auth.dto.AuditLogResponse;
import com.resumeai.auth.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLogResponse> latest(@RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String action) {
        return auditLogService.latest(actor, action);
    }
}