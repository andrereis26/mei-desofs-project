package com.desofs.project.document.model;

import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    @Test
    void builder_WithValidMetadata_CreatesDocument() {
        Document document = Document.builder()
                .filename(" report.pdf ")
                .filepath("/tmp/desofs/report.pdf")
                .contentType(" application/pdf ")
                .size(42L)
                .owner(owner())
                .build();

        assertThat(document.getFilename()).isEqualTo("report.pdf");
        assertThat(document.getContentType()).isEqualTo("application/pdf");
        assertThat(document.getCreatedAt()).isNotNull();
    }

    @Test
    void builder_WithBlankFilename_RejectsDocument() {
        assertThatThrownBy(() -> Document.builder()
                .filename(" ")
                .filepath("/tmp/desofs/report.pdf")
                .contentType("application/pdf")
                .size(42L)
                .owner(owner())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document filename is required");
    }

    @Test
    void builder_WithPathSegmentFilename_RejectsDocument() {
        assertThatThrownBy(() -> Document.builder()
                .filename("../secret.txt")
                .filepath("/tmp/desofs/secret.txt")
                .contentType("text/plain")
                .size(1L)
                .owner(owner())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filename cannot contain path segments");
    }

    @Test
    void builder_WithNegativeSize_RejectsDocument() {
        assertThatThrownBy(() -> Document.builder()
                .filename("report.pdf")
                .filepath("/tmp/desofs/report.pdf")
                .contentType("application/pdf")
                .size(-1L)
                .owner(owner())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size cannot be negative");
    }

    @Test
    void builder_WithoutOwner_RejectsDocument() {
        assertThatThrownBy(() -> Document.builder()
                .filename("report.pdf")
                .filepath("/tmp/desofs/report.pdf")
                .contentType("application/pdf")
                .size(42L)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document owner is required");
    }

    private User owner() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("owner")
                .role(Role.USER)
                .build();
    }
}
