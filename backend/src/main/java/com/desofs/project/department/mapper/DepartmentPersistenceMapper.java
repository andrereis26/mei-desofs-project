package com.desofs.project.department.mapper;

import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.department.model.Department;
import com.desofs.project.user.dbSchema.UserEntity;
import com.desofs.project.user.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DepartmentPersistenceMapper {

    public Department toDomain(DepartmentEntity entity) {
        if (entity == null) {
            return null;
        }
        Set<User> managers = entity.getManagers() == null ? Set.of() : entity.getManagers().stream()
                .map(this::toDomainUserShallow)
                .collect(Collectors.toSet());
        return Department.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .managers(managers)
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DepartmentEntity toEntity(Department domain) {
        if (domain == null) {
            return null;
        }
        Set<UserEntity> managers = domain.getManagers() == null ? Set.of() : domain.getManagers().stream()
                .map(this::toUserEntityRef)
                .collect(Collectors.toSet());
        return DepartmentEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .managers(managers)
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private User toDomainUserShallow(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User.UserBuilder builder = User.builder()
                .id(entity.getId())
                .role(entity.getRole());

        if (entity.getUsername() != null) {
            builder.username(entity.getUsername());
        }

        return builder.build();
    }

    private UserEntity toUserEntityRef(User user) {
        return UserEntity.builder().id(user.getId()).build();
    }
}
