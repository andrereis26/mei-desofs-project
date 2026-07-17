package com.desofs.project.audit.model;

import com.desofs.project.shared.validation.BoundedText;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.desofs.project.audit.model.AuditTextField.ACTION;
import static com.desofs.project.audit.model.AuditTextField.DETAILS;
import static com.desofs.project.audit.model.AuditTextField.ENTITY_ID;
import static com.desofs.project.audit.model.AuditTextField.ENTITY_TYPE;

public record AuditFields(
        String action,
        String entityType,
        String entityId,
        UUID userId,
        String details,
        LocalDateTime timestamp) {

    public AuditFields {
        action = ACTION.required(action);
        entityType = ENTITY_TYPE.required(entityType);
        entityId = ENTITY_ID.required(entityId);
        userId = requireActor(userId);
        details = DETAILS.optional(details);
        timestamp = requireTimestamp(timestamp);
    }

    public static AuditFields create(String action, String entityType, String entityId, UUID userId, String details) {
        return new AuditFields(action, entityType, entityId, userId, details, LocalDateTime.now());
    }

    private static UUID requireActor(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Audit actor is required");
        }
        return userId;
    }

    private static LocalDateTime requireTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Audit timestamp is required");
        }
        return timestamp;
    }
}
