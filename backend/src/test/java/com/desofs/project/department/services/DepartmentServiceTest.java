package com.desofs.project.department.services;

import com.desofs.project.audit.services.IAuditService;
import com.desofs.project.department.dtos.DepartmentDto;
import com.desofs.project.department.dtos.DepartmentRequest;
import com.desofs.project.department.mapper.DepartmentDtoMapper;
import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.shared.exceptions.DepartmentNameAlreadyExistsException;
import com.desofs.project.shared.exceptions.DepartmentNotFoundException;
import com.desofs.project.shared.exceptions.ManagersNotFoundException;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IAuditService auditService;

    @Mock
    private DepartmentDtoMapper departmentDtoMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private User managerUser;
    private Department department;

    @BeforeEach
    void setUp() {
        managerUser = User.builder()
                .id(UUID.randomUUID())
                .username("manager1")
                .email("manager@example.com")
                .password("pwd")
                .role(Role.MANAGER)
                .build();

        department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .description("Eng")
                .createdBy(managerUser.getId())
                .managers(Set.of(managerUser))
                .build();
    }

    @Test
    void create_AsManagerWithoutManagerIds_AddsCurrentManager() {
        DepartmentRequest request = new DepartmentRequest("Engineering", "Eng", null);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(managerUser));
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(userRepository.findAll()).thenReturn(List.of(managerUser));
        when(departmentDtoMapper.toDto(eq(department), anySet())).thenReturn(DepartmentDto.builder()
                .id(department.getId())
                .name("Engineering")
                .managers(Set.of())
                .build());

        DepartmentDto result = departmentService.create(request, "manager1");

        assertThat(result.getName()).isEqualTo("Engineering");
        assertThat(result.getManagers()).isNotNull();
        verify(auditService).log(eq("CREATE"), eq("Department"), eq(department.getId().toString()),
                eq(managerUser.getId()), contains("Engineering"));
    }

    @Test
    void joinDepartment_AddsMembershipToUser() {
        User normalUser = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .password("pwd")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(normalUser));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(userRepository.findAll()).thenReturn(List.of(normalUser));
        when(departmentDtoMapper.toDto(eq(department), anySet())).thenReturn(DepartmentDto.builder().id(department.getId()).name(department.getName()).build());

        DepartmentDto result = departmentService.joinDepartment(department.getId(), "user1");

        assertThat(result.getId()).isEqualTo(department.getId());
        verify(userRepository).save(normalUser);
        verify(auditService).log(eq("JOIN"), eq("Department"), eq(department.getId().toString()),
                eq(normalUser.getId()), contains("Engineering"));
    }

    @Test
    void update_AsNonManagerOfDepartment_ThrowsSecurityException() {
        User otherManager = User.builder()
                .id(UUID.randomUUID())
                .username("manager2")
                .email("manager2@example.com")
                .password("pwd")
                .role(Role.MANAGER)
                .build();

        DepartmentRequest request = new DepartmentRequest("Engineering", "Updated", Set.of(otherManager.getId()));

        when(userRepository.findByUsername("manager2")).thenReturn(Optional.of(otherManager));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> departmentService.update(department.getId(), request, "manager2"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void create_AsUser_ThrowsAccessDenied() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .role(Role.USER)
                .build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> departmentService.create(new DepartmentRequest("Ops", "Ops", null), "user"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_WhenDepartmentNameAlreadyExists_ThrowsConflict() {
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(managerUser));
        when(departmentRepository.existsByName("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(
                new DepartmentRequest("Engineering", "Eng", null), "manager1"))
                .isInstanceOf(DepartmentNameAlreadyExistsException.class);
    }

    @Test
    void create_WhenManagerIdsAreMissing_ThrowsManagersNotFound() {
        UUID missingId = UUID.randomUUID();
        DepartmentRequest request = new DepartmentRequest("Engineering", "Eng", Set.of(missingId));

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(managerUser));
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(userRepository.findAllById(Set.of(missingId))).thenReturn(List.of());

        assertThatThrownBy(() -> departmentService.create(request, "manager1"))
                .isInstanceOf(ManagersNotFoundException.class);
    }

    @Test
    void create_AsManagerWithoutSelfInManagerIds_ThrowsAccessDenied() {
        User otherManager = User.builder()
                .id(UUID.randomUUID())
                .username("manager2")
                .role(Role.MANAGER)
                .build();
        DepartmentRequest request = new DepartmentRequest("Engineering", "Eng", Set.of(otherManager.getId()));

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(managerUser));
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(userRepository.findAllById(Set.of(otherManager.getId()))).thenReturn(List.of(otherManager));

        assertThatThrownBy(() -> departmentService.create(request, "manager1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listAndFindMethods_ReturnMappedDtos() {
        DepartmentDto dto = DepartmentDto.builder().id(department.getId()).name("Engineering").build();
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(userRepository.findAll()).thenReturn(List.of(managerUser));
        when(departmentDtoMapper.toDto(eq(department), anySet())).thenReturn(dto);
        when(departmentRepository.listDepartments(eq("Eng"), any())).thenReturn(
                new PageImpl<>(List.of(department), PageRequest.of(0, 1), 1));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(department));

        assertThat(departmentService.listAll()).containsExactly(dto);
        PageResponse<DepartmentDto> page = departmentService.listDepartments(" Eng ", PageRequest.of(0, 1));
        assertThat(page.getContent()).containsExactly(dto);
        assertThat(departmentService.findById(department.getId())).isEqualTo(dto);
        assertThat(departmentService.findByName("Engineering")).isEqualTo(dto);
    }

    @Test
    void findByName_WhenMissing_ThrowsNotFound() {
        when(departmentRepository.findByName("Missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findByName("Missing"))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void update_AsAdmin_UpdatesDepartmentManagersAndAudits() {
        User admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        User otherManager = User.builder().id(UUID.randomUUID()).username("manager2").role(Role.MANAGER).build();
        DepartmentRequest request = new DepartmentRequest("Platform", "Platform team", Set.of(otherManager.getId()));
        Department saved = Department.builder()
                .id(department.getId())
                .name("Platform")
                .description("Platform team")
                .managers(Set.of(otherManager))
                .createdBy(managerUser.getId())
                .build();
        DepartmentDto dto = DepartmentDto.builder().id(department.getId()).name("Platform").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(departmentRepository.existsByName("Platform")).thenReturn(false);
        when(userRepository.findAllById(Set.of(otherManager.getId()))).thenReturn(List.of(otherManager));
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);
        when(userRepository.findAll()).thenReturn(List.of(otherManager));
        when(departmentDtoMapper.toDto(eq(saved), anySet())).thenReturn(dto);

        assertThat(departmentService.update(department.getId(), request, "admin").getName()).isEqualTo("Platform");
        verify(auditService).log(eq("UPDATE"), eq("Department"), eq(department.getId().toString()),
                eq(admin.getId()), contains("Platform"));
    }

    @Test
    void delete_AsDepartmentManager_DeletesAndAudits() {
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(managerUser));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));

        departmentService.delete(department.getId(), "manager1");

        verify(departmentRepository).delete(department);
        verify(auditService).log(eq("DELETE"), eq("Department"), eq(department.getId().toString()),
                eq(managerUser.getId()), contains("Engineering"));
    }
}
