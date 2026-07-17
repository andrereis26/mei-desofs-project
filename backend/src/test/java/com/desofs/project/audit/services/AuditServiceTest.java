package com.desofs.project.audit.services;

import com.desofs.project.audit.model.AuditLog;
import com.desofs.project.audit.repositories.AuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void log_WithCompleteEvent_AppendsAuditEntry() {
        UUID actorId = UUID.randomUUID();
        when(auditRepository.save(org.mockito.ArgumentMatchers.any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditService.log("CREATE", "Department", UUID.randomUUID().toString(), actorId, "Department created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void log_WithoutActor_RejectsEvent() {
        assertThatThrownBy(() -> auditService.log("CREATE", "Department", UUID.randomUUID().toString(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor");
    }

    @Test
    void log_WithoutTarget_RejectsEvent() {
        assertThatThrownBy(() -> auditService.log("CREATE", "Department", "", UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action and target");
    }
}
