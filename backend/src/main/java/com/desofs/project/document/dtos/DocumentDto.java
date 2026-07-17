package com.desofs.project.document.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private UUID id;
    private String filename;
    private String contentType;
    private long size;
    private UUID ownerId;
    private String ownerUsername;
    private UUID departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
}
