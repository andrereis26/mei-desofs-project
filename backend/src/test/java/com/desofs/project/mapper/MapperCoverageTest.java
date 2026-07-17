package com.desofs.project.mapper;

import com.desofs.project.audit.dbSchema.AuditLogEntity;
import com.desofs.project.audit.mapper.AuditPersistenceMapper;
import com.desofs.project.audit.model.AuditLog;
import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.department.dtos.DepartmentDto;
import com.desofs.project.department.mapper.DepartmentDtoMapper;
import com.desofs.project.department.mapper.DepartmentPersistenceMapper;
import com.desofs.project.department.model.Department;
import com.desofs.project.document.dbSchema.DocumentEntity;
import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.document.mapper.DocumentDtoMapper;
import com.desofs.project.document.mapper.DocumentPersistenceMapper;
import com.desofs.project.document.model.Document;
import com.desofs.project.user.dbSchema.UserEntity;
import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.mapper.UserDtoMapper;
import com.desofs.project.user.mapper.UserPersistenceMapper;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapperCoverageTest {

    private final UserPersistenceMapper userPersistenceMapper = new UserPersistenceMapper();
    private final DepartmentPersistenceMapper departmentPersistenceMapper = new DepartmentPersistenceMapper();
    private final DocumentPersistenceMapper documentPersistenceMapper = new DocumentPersistenceMapper();
    private final AuditPersistenceMapper auditPersistenceMapper = new AuditPersistenceMapper();
    private final UserDtoMapper userDtoMapper = Mappers.getMapper(UserDtoMapper.class);
    private final DepartmentDtoMapper departmentDtoMapper = Mappers.getMapper(DepartmentDtoMapper.class);
    private final DocumentDtoMapper documentDtoMapper = Mappers.getMapper(DocumentDtoMapper.class);

    @Test
    void persistenceMappers_ReturnNullForNullInputs() {
        assertThat(userPersistenceMapper.toDomain(null)).isNull();
        assertThat(userPersistenceMapper.toEntity(null)).isNull();
        assertThat(departmentPersistenceMapper.toDomain(null)).isNull();
        assertThat(departmentPersistenceMapper.toEntity(null)).isNull();
        assertThat(documentPersistenceMapper.toDomain(null)).isNull();
        assertThat(documentPersistenceMapper.toEntity(null)).isNull();
        assertThat(auditPersistenceMapper.toDomain(null)).isNull();
        assertThat(auditPersistenceMapper.toEntity(null)).isNull();
    }

    @Test
    void userPersistenceMapper_MapsDepartmentsAndHandlesLazyInitialization() {
        UUID departmentId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.MANAGER)
                .departments(Set.of(DepartmentEntity.builder().id(departmentId).name("Engineering").build()))
                .createdAt(LocalDateTime.now())
                .build();

        User domain = userPersistenceMapper.toDomain(entity);
        UserEntity mappedEntity = userPersistenceMapper.toEntity(domain);

        assertThat(domain.getDepartments()).extracting(Department::getId).containsExactly(departmentId);
        assertThat(mappedEntity.getDepartments()).extracting(DepartmentEntity::getId).containsExactly(departmentId);

        UserEntity lazyEntity = mock(UserEntity.class);
        when(lazyEntity.getId()).thenReturn(UUID.randomUUID());
        when(lazyEntity.getUsername()).thenReturn("lazy");
        when(lazyEntity.getEmail()).thenReturn("lazy@example.com");
        when(lazyEntity.getPassword()).thenReturn("encoded");
        when(lazyEntity.getRole()).thenReturn(Role.USER);
        when(lazyEntity.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(lazyEntity.getDepartments()).thenThrow(new LazyInitializationException("closed"));

        assertThat(userPersistenceMapper.toDomain(lazyEntity).getDepartments()).isEmpty();
    }

    @Test
    void departmentPersistenceMapper_MapsManagerReferences() {
        UUID managerId = UUID.randomUUID();
        DepartmentEntity entity = DepartmentEntity.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .description("Software")
                .createdBy(UUID.randomUUID())
                .updatedBy(UUID.randomUUID())
                .managers(Set.of(UserEntity.builder()
                        .id(managerId)
                        .username("manager")
                        .role(Role.MANAGER)
                        .build()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Department domain = departmentPersistenceMapper.toDomain(entity);
        DepartmentEntity mappedEntity = departmentPersistenceMapper.toEntity(domain);

        assertThat(domain.getManagers()).extracting(User::getId).containsExactly(managerId);
        assertThat(mappedEntity.getManagers()).extracting(UserEntity::getId).containsExactly(managerId);
    }

    @Test
    void documentPersistenceMapper_MapsOwnerDepartmentManagersAndMetadata() {
        UUID ownerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        DocumentEntity entity = DocumentEntity.builder()
                .id(UUID.randomUUID())
                .filename("report.pdf")
                .filepath("/tmp/report.pdf")
                .contentType("application/pdf")
                .size(42L)
                .owner(UserEntity.builder().id(ownerId).username("owner").role(Role.USER).build())
                .department(DepartmentEntity.builder()
                        .id(departmentId)
                        .name("Engineering")
                        .managers(Set.of(UserEntity.builder().id(managerId).build()))
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        Document domain = documentPersistenceMapper.toDomain(entity);
        DocumentEntity mappedEntity = documentPersistenceMapper.toEntity(domain);

        assertThat(domain.getOwner().getId()).isEqualTo(ownerId);
        assertThat(domain.getDepartment().getManagers()).extracting(User::getId).containsExactly(managerId);
        assertThat(mappedEntity.getOwner().getId()).isEqualTo(ownerId);
        assertThat(mappedEntity.getDepartment().getId()).isEqualTo(departmentId);
    }

    @Test
    void auditPersistenceMapper_MapsRoundTrip() {
        AuditLogEntity entity = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .action("UPLOAD")
                .entityType("Document")
                .entityId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID())
                .details("uploaded")
                .timestamp(LocalDateTime.now())
                .build();

        AuditLog domain = auditPersistenceMapper.toDomain(entity);
        AuditLogEntity mappedEntity = auditPersistenceMapper.toEntity(domain);

        assertThat(domain.getAction()).isEqualTo("UPLOAD");
        assertThat(mappedEntity.getEntityId()).isEqualTo(domain.getEntityId());
    }

    @Test
    void dtoMappers_MapNestedValuesAndNullInputs() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .username("owner")
                .email("owner@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
        User manager = User.builder().id(UUID.randomUUID()).username("manager").role(Role.MANAGER).build();
        Department department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .description("Software")
                .managers(Set.of(manager))
                .createdAt(LocalDateTime.now())
                .build();
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .filename("report.pdf")
                .filepath("/tmp/report.pdf")
                .contentType("application/pdf")
                .size(42L)
                .owner(owner)
                .department(department)
                .createdAt(LocalDateTime.now())
                .build();

        UserDto userDto = userDtoMapper.toDto(owner);
        DepartmentDto departmentDto = departmentDtoMapper.toDto(department, Set.of(owner));
        DocumentDto documentDto = documentDtoMapper.toDto(document);
        assertThat(userDto.getRole()).isEqualTo("USER");
        assertThat(userDtoMapper.toDto(User.builder().username("norole").role(null).build()).getRole()).isEqualTo("USER");
        assertThat(departmentDto.getManagers()).extracting("username").containsExactly("manager");
        assertThat(departmentDto.getMembers()).extracting("username").containsExactly("owner");
        assertThat(departmentDtoMapper.usersToSummaries(null)).isEmpty();
        assertThat(documentDto.getOwnerId()).isEqualTo(owner.getId());
        assertThat(documentDto.getOwnerUsername()).isEqualTo("owner");
        assertThat(documentDto.getDepartmentId()).isEqualTo(department.getId());
        assertThat(documentDto.getDepartmentName()).isEqualTo("Engineering");
        assertThat(userDtoMapper.toDto(null)).isNull();
        assertThat(departmentDtoMapper.toDto(null).getMembers()).isEmpty();
        assertThat(documentDtoMapper.toDto(null)).isNull();
    }
}
