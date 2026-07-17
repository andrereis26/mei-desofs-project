package com.desofs.project.shared.exceptions;

public class DepartmentNotFoundException extends RuntimeException {

    private final String departmentId;

    public DepartmentNotFoundException(String identifier) {
        super("Department not found: " + identifier);
        this.departmentId = identifier;
    }

    public String getDepartmentId() {
        return departmentId;
    }
}