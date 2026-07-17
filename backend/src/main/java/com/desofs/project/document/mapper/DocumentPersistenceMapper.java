package com.desofs.project.document.mapper;

import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.department.model.Department;
import com.desofs.project.document.dbSchema.DocumentEntity;
import com.desofs.project.document.model.Document;
import com.desofs.project.document.model.DocumentMetadata;
import com.desofs.project.user.dbSchema.UserEntity;
import com.desofs.project.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceMapper {

    public Document toDomain(DocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Document.builder()
                .id(entity.getId())
                .metadata(new DocumentMetadata(
                        entity.getFilename(),
                        entity.getFilepath(),
                        entity.getContentType(),
                        entity.getSize()))
                .owner(toDomainUserShallow(entity.getOwner()))
                .department(toDomainDepartmentShallow(entity.getDepartment()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public DocumentEntity toEntity(Document domain) {
        if (domain == null) {
            return null;
        }
        return DocumentEntity.builder()
                .id(domain.getId())
                .filename(domain.getMetadata().filename())
                .filepath(domain.getMetadata().filepath())
                .contentType(domain.getMetadata().contentType())
                .size(domain.getMetadata().size())
                .owner(toUserEntityRef(domain.getOwner()))
                .department(toDepartmentEntityRef(domain.getDepartment()))
                .createdAt(domain.getCreatedAt())
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

    private Department toDomainDepartmentShallow(DepartmentEntity entity) {
        if (entity == null) {
            return null;
        }
        Department.DepartmentBuilder builder = Department.builder()
                .id(entity.getId())
                .managers(toDomainManagerRefs(entity));

        if (entity.getName() != null) {
            builder.name(entity.getName());
        }

        return builder.build();
    }

    private java.util.Set<User> toDomainManagerRefs(DepartmentEntity entity) {
        if (entity.getManagers() == null) {
            return java.util.Set.of();
        }
        return entity.getManagers().stream()
                .map(u -> User.builder().id(u.getId()).build())
                .collect(java.util.stream.Collectors.toSet());
    }

    private UserEntity toUserEntityRef(User domain) {
        if (domain == null) {
            return null;
        }
        return UserEntity.builder().id(domain.getId()).build();
    }

    private DepartmentEntity toDepartmentEntityRef(Department domain) {
        if (domain == null) {
            return null;
        }
        return DepartmentEntity.builder().id(domain.getId()).build();
    }
}
