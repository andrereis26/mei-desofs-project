package com.desofs.project.department.repositories;

import com.desofs.project.department.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
    Department save(Department department);
    List<Department> findAll();
    Page<Department> listDepartments(String name, Pageable pageable);
    Optional<Department> findById(UUID id);
    void delete(Department department);
    void deleteAll();
}
