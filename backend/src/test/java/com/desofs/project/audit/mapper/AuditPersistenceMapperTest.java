package com.desofs.project.audit.mapper;

import com.desofs.project.audit.dbSchema.AuditLogEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditPersistenceMapperTest {

    private final AuditPersistenceMapper mapper = new AuditPersistenceMapper();

    @Test
    void toDomain_WithIncompleteAuditFields_RejectsEntity() {
        AuditLogEntity entity = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .action("UPLOAD")
                .entityType("Document")
                .entityId("")
                .userId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Audit target id is required");
    }
}
