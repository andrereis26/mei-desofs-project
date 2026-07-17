package com.desofs.project.user.mapper;

import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.department.model.Department;
import com.desofs.project.user.dbSchema.UserEntity;
import com.desofs.project.user.model.User;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        Set<Department> departments = Set.of();
        try {
            departments = entity.getDepartments() == null ? Set.of() : entity.getDepartments().stream()
                    .map(this::toDomainDepartmentShallow)
                    .collect(Collectors.toSet());
        } catch (LazyInitializationException ignored) {
            log.debug("LazyInitializationException when mapping user departments for userId={}", entity.getId());
            departments = Set.of();
        }
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .departments(departments)
                .build();
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        Set<DepartmentEntity> departments = domain.getDepartments() == null ? Set.of() : domain.getDepartments().stream()
                .map(this::toDepartmentEntityRef)
                .collect(Collectors.toSet());
        return UserEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .role(domain.getRole())
                .createdAt(domain.getCreatedAt())
                .departments(departments)
                .build();
    }

    private Department toDomainDepartmentShallow(DepartmentEntity entity) {
        return Department.builder().id(entity.getId()).name(entity.getName()).build();
    }

    private DepartmentEntity toDepartmentEntityRef(Department domain) {
        return DepartmentEntity.builder().id(domain.getId()).build();
    }
}
