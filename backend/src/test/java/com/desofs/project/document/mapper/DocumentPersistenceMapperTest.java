package com.desofs.project.document.mapper;

import com.desofs.project.document.dbSchema.DocumentEntity;
import com.desofs.project.user.dbSchema.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentPersistenceMapperTest {

    private final DocumentPersistenceMapper mapper = new DocumentPersistenceMapper();

    @Test
    void toDomain_WithInvalidMetadata_RejectsEntity() {
        DocumentEntity entity = DocumentEntity.builder()
                .id(UUID.randomUUID())
                .filename("")
                .filepath("/tmp/desofs/report.pdf")
                .contentType("application/pdf")
                .size(42L)
                .owner(UserEntity.builder().id(UUID.randomUUID()).username("owner").build())
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document filename is required");
    }
}
