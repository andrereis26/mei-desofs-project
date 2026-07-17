package com.desofs.project.audit.repositories;

import com.desofs.project.audit.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuditRepositoryImplIT {

    @Autowired
    private AuditRepository auditRepository;

    @Test
    void save_AppendsNewAuditLog() {
        UUID actorId = UUID.randomUUID();

        AuditLog saved = auditRepository.save(AuditLog.builder()
                .action("CREATE")
                .entityType("Department")
                .entityId(UUID.randomUUID().toString())
                .userId(actorId)
                .details("Department created")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(actorId);
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void save_WithExistingId_RejectsMutation() {
        AuditLog existingLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .action("UPDATE")
                .entityType("Department")
                .entityId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID())
                .details("Attempted mutation")
                .build();

        assertThatThrownBy(() -> auditRepository.save(existingLog))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("append-only");
    }
}
