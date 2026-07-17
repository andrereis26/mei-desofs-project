package com.desofs.project.department.model;

import com.desofs.project.user.model.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class Department {
    private UUID id;
    private DepartmentName name;
    private DepartmentDescription description;
    private Set<User> managers = new HashSet<>();
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @Builder
    public Department(
            UUID id,
            DepartmentName name,
            DepartmentDescription description,
            Set<User> managers,
            UUID createdBy,
            UUID updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.managers = managers == null ? new HashSet<>() : new HashSet<>(managers);
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return name == null ? null : name.value();
    }

    public String getDescription() {
        return description == null ? null : description.value();
    }

    public void setName(String name) {
        this.name = DepartmentName.of(name);
    }

    public void setDescription(String description) {
        this.description = DepartmentDescription.of(description);
    }

    public void setManagers(Set<User> managers) {
        this.managers = managers == null ? new HashSet<>() : new HashSet<>(managers);
    }


    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class DepartmentBuilder {
        public DepartmentBuilder name(String name) {
            this.name = DepartmentName.of(name);
            return this;
        }

        public DepartmentBuilder description(String description) {
            this.description = DepartmentDescription.of(description);
            return this;
        }
    }
}
