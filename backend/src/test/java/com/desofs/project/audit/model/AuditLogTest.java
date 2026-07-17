package com.desofs.project.audit.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditLogTest {

    @Test
    void builder_WithCompleteFields_CreatesAuditLog() {
        UUID actorId = UUID.randomUUID();

        AuditLog log = AuditLog.builder()
                .action(" CREATE ")
                .entityType(" Document ")
                .entityId(" " + UUID.randomUUID() + " ")
                .userId(actorId)
                .details(" Document uploaded ")
                .build();

        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getEntityType()).isEqualTo("Document");
        assertThat(log.getUserId()).isEqualTo(actorId);
        assertThat(log.getDetails()).isEqualTo("Document uploaded");
        assertThat(log.getTimestamp()).isNotNull();
    }

    @Test
    void builder_WithBlankAction_RejectsAuditLog() {
        assertThatThrownBy(() -> AuditLog.builder()
                .action(" ")
                .entityType("Document")
                .entityId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Audit action is required");
    }

    @Test
    void builder_WithBlankTarget_RejectsAuditLog() {
        assertThatThrownBy(() -> AuditLog.builder()
                .action("UPLOAD")
                .entityType("Document")
                .entityId(" ")
                .userId(UUID.randomUUID())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Audit target id is required");
    }

    @Test
    void builder_WithoutActor_RejectsAuditLog() {
        assertThatThrownBy(() -> AuditLog.builder()
                .action("UPLOAD")
                .entityType("Document")
                .entityId(UUID.randomUUID().toString())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Audit actor is required");
    }

    @Test
    void fields_WithoutTimestamp_RejectsAuditFields() {
        assertThatThrownBy(() -> new AuditFields(
                "UPLOAD",
                "Document",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Audit timestamp is required");
    }

    @Test
    void fields_WithExplicitTimestamp_KeepsTimestamp() {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        AuditFields fields = new AuditFields(
                "UPLOAD",
                "Document",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                null,
                timestamp);

        assertThat(fields.timestamp()).isEqualTo(timestamp);
    }
}
