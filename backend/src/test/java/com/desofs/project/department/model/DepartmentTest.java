package com.desofs.project.department.model;

import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepartmentTest {

    @Test
    void builder_WithValidStringValues_CreatesDepartmentWithNormalizedValues() {
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Department department = Department.builder()
                .name(" Security ")
                .description(" Application security ")
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertThat(department.getName()).isEqualTo("Security");
        assertThat(department.getDescription()).isEqualTo("Application security");
        assertThat(department.getCreatedBy()).isEqualTo(createdBy);
        assertThat(department.getUpdatedBy()).isEqualTo(updatedBy);
        assertThat(department.getCreatedAt()).isEqualTo(createdAt);
        assertThat(department.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(department.getManagers()).isEmpty();
    }

    @Test
    void builder_WithNullDescription_AllowsOptionalDescription() {
        Department department = Department.builder()
                .name("Security")
                .description((String)null)
                .build();

        assertThat(department.getDescription()).isNull();
    }



    @Test
    void builder_WithNullName_RejectsDepartment() {
        assertThatThrownBy(() -> Department.builder()
                .name((String)null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Department name is required");
    }

    @Test
    void builder_WithBlankName_RejectsDepartment() {
        assertThatThrownBy(() -> Department.builder()
                .name(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Department name is required");
    }

    @Test
    void builder_WithMaximumLengthName_CreatesDepartment() {
        String name = "a".repeat(100);

        Department department = Department.builder()
                .name(name)
                .build();

        assertThat(department.getName()).isEqualTo(name);
    }

    @Test
    void builder_WithTooLongName_RejectsDepartment() {
        assertThatThrownBy(() -> Department.builder()
                .name("a".repeat(101))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Department name cannot exceed 100 characters");
    }

    @Test
    void builder_WithBlankDescription_RejectsDepartment() {
        assertThatThrownBy(() -> Department.builder()
                .name("Security")
                .description(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Department description must not be blank");
    }

    @Test
    void builder_WithMaximumLengthDescription_CreatesDepartment() {
        String description = "a".repeat(1000);

        Department department = Department.builder()
                .name("Security")
                .description(description)
                .build();

        assertThat(department.getDescription()).isEqualTo(description);
    }

    @Test
    void builder_WithTooLongDescription_RejectsDepartment() {
        assertThatThrownBy(() -> Department.builder()
                .name("Security")
                .description("a".repeat(1001))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Department description cannot exceed 1000 characters");
    }





    private User manager() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("manager")
                .email("manager@example.com")
                .password("encoded-password")
                .role(Role.MANAGER)
                .build();
    }
}
