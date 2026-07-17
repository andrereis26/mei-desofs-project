package com.desofs.project.audit.mapper;

import com.desofs.project.audit.dbSchema.AuditLogEntity;
import com.desofs.project.audit.model.AuditFields;
import com.desofs.project.audit.model.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditPersistenceMapper {
    public AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditLog.builder()
                .id(entity.getId())
                .fields(new AuditFields(
                        entity.getAction(),
                        entity.getEntityType(),
                        entity.getEntityId(),
                        entity.getUserId(),
                        entity.getDetails(),
                        entity.getTimestamp()))
                .build();
    }

    public AuditLogEntity toEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        return AuditLogEntity.builder()
                .id(log.getId())
                .action(log.getFields().action())
                .entityType(log.getFields().entityType())
                .entityId(log.getFields().entityId())
                .userId(log.getFields().userId())
                .details(log.getFields().details())
                .timestamp(log.getFields().timestamp())
                .build();
    }
}
