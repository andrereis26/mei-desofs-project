package com.desofs.project.audit.services;

import com.desofs.project.audit.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface IAuditService {
    AuditLog log(String action, String entityType, String entityId, UUID userId, String details);
    List<AuditLog> findAll();
    List<AuditLog> findByUserId(UUID userId);
}
