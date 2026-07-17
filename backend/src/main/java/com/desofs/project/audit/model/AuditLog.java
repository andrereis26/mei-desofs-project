package com.desofs.project.audit.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class AuditLog {
    private UUID id;
    private AuditFields fields;

    @Builder
    public AuditLog(
            UUID id,
            AuditFields fields,
            String action,
            String entityType,
            String entityId,
            UUID userId,
            String details,
            LocalDateTime timestamp) {
        this.id = id;
        this.fields = fields == null
                ? buildFields(action, entityType, entityId, userId, details, timestamp)
                : fields;
    }

    public String getAction() {
        return fields.action();
    }

    public String getEntityType() {
        return fields.entityType();
    }

    public String getEntityId() {
        return fields.entityId();
    }

    public UUID getUserId() {
        return fields.userId();
    }

    public String getDetails() {
        return fields.details();
    }

    public LocalDateTime getTimestamp() {
        return fields.timestamp();
    }

    private static AuditFields buildFields(
            String action,
            String entityType,
            String entityId,
            UUID userId,
            String details,
            LocalDateTime timestamp) {
        if (timestamp == null) {
            return AuditFields.create(action, entityType, entityId, userId, details);
        }
        return new AuditFields(action, entityType, entityId, userId, details, timestamp);
    }
}
