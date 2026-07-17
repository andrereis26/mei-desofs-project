package com.desofs.project.shared.exceptions;

public class DepartmentNameAlreadyExistsException extends RuntimeException {

    private final String departmentName;

    public DepartmentNameAlreadyExistsException(String departmentName) {
        super("Department name already exists: " + departmentName);
        this.departmentName = departmentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}
