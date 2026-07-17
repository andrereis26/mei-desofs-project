package com.desofs.project.department.model;

import com.desofs.project.shared.validation.BoundedText;

public record DepartmentName(String value) {

    private static final int MAX_LENGTH = 100;

    public DepartmentName {
        value = BoundedText.required(value, "Department name", MAX_LENGTH);
    }

    public static DepartmentName of(String value) {
        return new DepartmentName(value);
    }
}
