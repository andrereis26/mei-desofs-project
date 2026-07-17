package com.desofs.project.document.dbSchema;

import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.user.dbSchema.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @NotBlank
    @Size(max = 500)
    @Column(name = "filepath", nullable = false, length = 500)
    private String filepath;

    @Size(max = 100)
    @Column(name = "content_type", length = 100)
    private String contentType;

    @PositiveOrZero
    @Column(name = "size", nullable = false)
    private long size;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
