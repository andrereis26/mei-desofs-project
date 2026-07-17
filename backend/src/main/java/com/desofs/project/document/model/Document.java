package com.desofs.project.document.model;

import com.desofs.project.department.model.Department;
import com.desofs.project.user.model.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public final class Document {
    private UUID id;
    private DocumentMetadata metadata;
    private User owner;
    private Department department;
    private LocalDateTime createdAt;

    @Builder
    public Document(
            UUID id,
            DocumentMetadata metadata,
            String filename,
            String filepath,
            String contentType,
            Long size,
            User owner,
            Department department,
            LocalDateTime createdAt) {
        this.id = id;
        this.metadata = metadata == null ? DocumentMetadata.of(filename, filepath, contentType, size) : metadata;
        this.owner = requireOwner(owner);
        this.department = department;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public String getFilename() {
        return metadata.filename();
    }

    public String getFilepath() {
        return metadata.filepath();
    }

    public String getContentType() {
        return metadata.contentType();
    }

    public long getSize() {
        return metadata.size();
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setMetadata(DocumentMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Document metadata is required");
        }
        this.metadata = metadata;
    }

    public void setFilename(String filename) {
        setMetadata(DocumentMetadata.of(filename, getFilepath(), getContentType(), getSize()));
    }

    public void setFilepath(String filepath) {
        setMetadata(DocumentMetadata.of(getFilename(), filepath, getContentType(), getSize()));
    }

    public void setContentType(String contentType) {
        setMetadata(DocumentMetadata.of(getFilename(), getFilepath(), contentType, getSize()));
    }

    public void setSize(long size) {
        setMetadata(DocumentMetadata.of(getFilename(), getFilepath(), getContentType(), size));
    }

    public void setOwner(User owner) {
        this.owner = requireOwner(owner);
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Document creation timestamp is required");
        }
        this.createdAt = createdAt;
    }

    private static User requireOwner(User owner) {
        if (owner == null || owner.getId() == null) {
            throw new IllegalArgumentException("Document owner is required");
        }
        return owner;
    }
}
