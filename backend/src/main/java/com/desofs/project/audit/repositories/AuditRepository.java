package com.desofs.project.audit.repositories;

import com.desofs.project.audit.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditRepository {
    AuditLog save(AuditLog log);
    List<AuditLog> findAll();
    List<AuditLog> findByUserId(UUID userId);
}
