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
import com.desofs.project.shared.exceptions.UserNotFoundException;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService implements IDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final IAuditService auditService;
    private final DepartmentDtoMapper departmentDtoMapper;

    @Override
    @Transactional
    public DepartmentDto create(DepartmentRequest request, String username) {
        User currentUser = getUserByUsername(username);
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Access denied");
        }
        if (departmentRepository.existsByName(request.getName())) {
            throw new DepartmentNameAlreadyExistsException(request.getName());
        }

        Set<User> managers = resolveManagers(request.getManagerIds(), currentUser);

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .managers(managers)
                .createdBy(currentUser.getId())
                .updatedBy(currentUser.getId())
                .updatedAt(LocalDateTime.now())
                .build();

        Department saved = departmentRepository.save(department);
        auditService.log("CREATE", "Department", saved.getId().toString(), currentUser.getId(),
                "Department created: " + saved.getName());

        return toDepartmentDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> listAll() {
        return departmentRepository.findAll().stream().map(this::toDepartmentDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentDto> listDepartments(String name, Pageable pageable) {
        String normalizedName = name == null || name.isBlank() ? null : name.trim();
        return PageResponse.from(departmentRepository.listDepartments(normalizedName, pageable)
                .map(this::toDepartmentDto));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto findById(UUID id) {
        return toDepartmentDto(getDepartmentById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto findByName(String name) {
        Department department = departmentRepository.findByName(name)
                .orElseThrow(() -> new DepartmentNotFoundException(name));
        return toDepartmentDto(department);
    }

    @Override
    @Transactional
    public DepartmentDto update(UUID id, DepartmentRequest request, String username) {
        User currentUser = getUserByUsername(username);
        Department department = getDepartmentById(id);

        enforceDepartmentManagementPermission(currentUser, department);

        if (!department.getName().equals(request.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new DepartmentNameAlreadyExistsException(request.getName());
        }

        department.setName(request.getName());
        department.setDescription(request.getDescription());

        if (request.getManagerIds() != null) {
            department.setManagers(resolveManagers(request.getManagerIds(), currentUser));
        }

        department.setUpdatedBy(currentUser.getId());
        department.setUpdatedAt(LocalDateTime.now());

        Department saved = departmentRepository.save(department);
        auditService.log("UPDATE", "Department", saved.getId().toString(), currentUser.getId(),
                "Department updated: " + saved.getName());

        return toDepartmentDto(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, String username) {
        User currentUser = getUserByUsername(username);
        Department department = getDepartmentById(id);

        enforceDepartmentManagementPermission(currentUser, department);

        departmentRepository.delete(department);
        auditService.log("DELETE", "Department", id.toString(), currentUser.getId(),
                "Department deleted: " + department.getName());
    }

    @Override
    @Transactional
    public DepartmentDto joinDepartment(UUID id, String username) {
        User currentUser = getUserByUsername(username);
        Department department = getDepartmentById(id);

        currentUser.getDepartments().add(department);
        userRepository.save(currentUser);

        auditService.log("JOIN", "Department", id.toString(), currentUser.getId(),
                "User joined department: " + department.getName());

        return toDepartmentDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isManagerOfDepartment(User user, Department department) {
        return department.getManagers().stream().anyMatch(manager -> manager.getId().equals(user.getId()));
    }

    private void enforceDepartmentManagementPermission(User currentUser, Department department) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.MANAGER || !isManagerOfDepartment(currentUser, department)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private Set<User> resolveManagers(Set<UUID> managerIds, User currentUser) {
        Set<User> managers = new HashSet<>();

        if (managerIds == null || managerIds.isEmpty()) {
            if (currentUser.getRole() == Role.MANAGER) {
                managers.add(currentUser);
            }
            return managers;
        }

        List<User> found = userRepository.findAllById(managerIds);
        if (found.size() != managerIds.size()) {
            Set<UUID> foundIds = found.stream().map(User::getId).collect(Collectors.toSet());
            Set<UUID> missingIds = new HashSet<>(managerIds);
            missingIds.removeAll(foundIds);
            throw new ManagersNotFoundException(missingIds);
        }

        for (User user : found) {
            if (user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("Access denied");
            }
        }

        if (currentUser.getRole() == Role.MANAGER) {
            boolean containsCurrent = found.stream().anyMatch(u -> u.getId().equals(currentUser.getId()));
            if (!containsCurrent) {
                throw new AccessDeniedException("Access denied");
            }
        }

        managers.addAll(found);
        return managers;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
    }

    private Department getDepartmentById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id.toString()));
    }

    private DepartmentDto toDepartmentDto(Department department) {
        Set<User> members = resolveMembers(department);
        return departmentDtoMapper.toDto(department, members);
    }

    private Set<User> resolveMembers(Department department) {
        if (department == null || department.getId() == null) {
            return Set.of();
        }

        UUID departmentId = department.getId();
        return userRepository.findAll().stream()
                .filter(user -> user.getDepartments() != null)
                .filter(user -> user.getDepartments().stream().anyMatch(dep -> departmentId.equals(dep.getId())))
                .collect(Collectors.toSet());
    }
}
