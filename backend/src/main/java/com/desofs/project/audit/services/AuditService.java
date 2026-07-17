package com.desofs.project.audit.services;

import com.desofs.project.audit.model.AuditLog;
import com.desofs.project.audit.repositories.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService implements IAuditService {

    private final AuditRepository auditRepository;

    @Override
    @Transactional
    public AuditLog log(String action, String entityType, String entityId, UUID userId, String details) {
        if (isBlank(action) || isBlank(entityType) || isBlank(entityId)) {
            throw new IllegalArgumentException("Audit action and target are required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Audit actor is required");
        }

        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .details(details)
                .build();
        return auditRepository.save(log);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findAll() {
        return auditRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByUserId(UUID userId) {
        return auditRepository.findByUserId(userId);
    }
}
