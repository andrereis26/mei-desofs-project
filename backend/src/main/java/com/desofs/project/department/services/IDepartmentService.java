package com.desofs.project.department.services;

import com.desofs.project.department.dtos.DepartmentDto;
import com.desofs.project.department.dtos.DepartmentRequest;
import com.desofs.project.department.model.Department;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.user.model.User;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IDepartmentService {
    DepartmentDto create(DepartmentRequest request, String username);
    List<DepartmentDto> listAll();
    PageResponse<DepartmentDto> listDepartments(String name, Pageable pageable);
    DepartmentDto findById(UUID id);
    DepartmentDto findByName(String name);
    DepartmentDto update(UUID id, DepartmentRequest request, String username);
    void delete(UUID id, String username);
    DepartmentDto joinDepartment(UUID id, String username);
    boolean isManagerOfDepartment(User user, Department department);
}
