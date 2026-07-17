package com.desofs.project.department.model;

import com.desofs.project.shared.validation.BoundedText;

public record DepartmentDescription(String value) {

    private static final int MAX_LENGTH = 1000;

    public DepartmentDescription {
        if (value != null) {
            if (value.isBlank()) {
                throw new IllegalArgumentException("Department description must not be blank");
            }
            value = BoundedText.required(value, "Department description", MAX_LENGTH);
        }
    }

    public static DepartmentDescription of(String value) {
        return value == null ? null : new DepartmentDescription(value);
    }
}
