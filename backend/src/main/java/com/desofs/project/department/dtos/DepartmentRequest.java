package com.desofs.project.department.dtos;

import com.desofs.project.shared.validation.NotBlankIfPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {
    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must be less than or equal to 100 characters")
    private String name;
    @NotBlankIfPresent(message = "description must not be blank")
    @Size(max = 1000, message = "description must be less than or equal to 1000 characters")
    private String description;
    @Size(max = 100)
    private Set<UUID> managerIds;
}
