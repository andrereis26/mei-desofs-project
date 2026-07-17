package com.desofs.project.config;

import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("!test & !prod")
@RequiredArgsConstructor
public class StartupDataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDataBootstrap.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User admin = ensureUser("bootstrap_admin", "bootstrap_admin@desofs.local", Role.ADMIN);
        User managerA = ensureUser("bootstrap_manager", "bootstrap_manager@desofs.local", Role.MANAGER);
        User managerB = ensureUser("bootstrap_manager_2", "bootstrap_manager_2@desofs.local", Role.MANAGER);
        User managerC = ensureUser("bootstrap_manager_3", "bootstrap_manager_3@desofs.local", Role.MANAGER);

        User userA = ensureUser("bootstrap_user", "bootstrap_user@desofs.local", Role.USER);
        User userB = ensureUser("bootstrap_user_2", "bootstrap_user_2@desofs.local", Role.USER);
        User userC = ensureUser("bootstrap_user_3", "bootstrap_user_3@desofs.local", Role.USER);
        User userD = ensureUser("bootstrap_user_4", "bootstrap_user_4@desofs.local", Role.USER);

        UUID actorId = resolveAdminId(admin);
        if (actorId == null) {
            log.warn("Skipping bootstrap departments because no ADMIN user is available");
            return;
        }

        Department engineering = ensureDepartment("Engineering", "Software and platform engineering", actorId);
        Department operations = ensureDepartment("Operations", "Operations and infrastructure", actorId);
        Department compliance = ensureDepartment("Compliance", "Security and compliance governance", actorId);

        addManagersToDepartment(engineering, actorId, admin, managerA, managerB);
        addManagersToDepartment(operations, actorId, admin, managerB, managerC);
        addManagersToDepartment(compliance, actorId, admin, managerA, managerC);

        addMembersToDepartment(engineering, managerA, managerB, userA, userB);
        addMembersToDepartment(operations, managerB, managerC, userB, userC);
        addMembersToDepartment(compliance, managerA, managerC, userC, userD);
    }

    private User ensureUser(String username, String email, Role role) {
        if (userRepository.existsByUsername(username)) {
            log.debug("Bootstrap user already exists by username: {}", username);
            return userRepository.findByUsername(username).orElse(null);
        }
        if (userRepository.existsByEmail(email)) {
            log.debug("Bootstrap user already exists by email: {}", email);
            return userRepository.findByEmail(email).orElse(null);
        }

        User saved = userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("ChangeMe123!"))
                .role(role)
                .build());

        log.info("Bootstrap user created: {} ({})", saved.getUsername(), saved.getRole());
        return saved;
    }

    private UUID resolveAdminId(User bootstrapAdmin) {
        if (bootstrapAdmin != null) {
            return bootstrapAdmin.getId();
        }

        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .map(User::getId)
                .findFirst()
                .orElse(null);
    }

    private Department ensureDepartment(String name, String description, UUID actorId) {
        Department existing = departmentRepository.findByName(name).orElse(null);
        if (existing != null) {
            log.debug("Bootstrap department already exists: {}", name);
            return existing;
        }

        Department saved = departmentRepository.save(Department.builder()
                .name(name)
                .description(description)
                .createdBy(actorId)
                .updatedBy(actorId)
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("Bootstrap department created: {}", saved.getName());
        return saved;
    }

    private void addManagersToDepartment(Department department, UUID actorId, User... managersToAdd) {
        if (department == null) {
            return;
        }

        Set<User> managers = department.getManagers() == null
                ? new HashSet<>()
                : new HashSet<>(department.getManagers());
        boolean changed = false;

        for (User candidate : managersToAdd) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }

            boolean alreadyManager = managers.stream().anyMatch(existing -> candidate.getId().equals(existing.getId()));
            if (!alreadyManager) {
                managers.add(candidate);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        department.setManagers(managers);
        department.setUpdatedBy(actorId);
        department.setUpdatedAt(LocalDateTime.now());
        departmentRepository.save(department);
        log.info("Bootstrap managers updated for department: {}", department.getName());
    }

    private void addMembersToDepartment(Department department, User... usersToAdd) {
        if (department == null || department.getId() == null) {
            return;
        }

        List<String> updatedUsers = new ArrayList<>();

        for (User seedUser : usersToAdd) {
            if (seedUser == null || seedUser.getUsername() == null) {
                continue;
            }

            User persistedUser = userRepository.findByUsername(seedUser.getUsername()).orElse(null);
            if (persistedUser == null) {
                continue;
            }

            Set<Department> memberships = persistedUser.getDepartments() == null
                    ? new HashSet<>()
                    : new HashSet<>(persistedUser.getDepartments());

            boolean alreadyMember = memberships.stream()
                    .anyMatch(existingDept -> department.getId().equals(existingDept.getId()));
            if (alreadyMember) {
                continue;
            }

            memberships.add(Department.builder().id(department.getId()).build());
            persistedUser.setDepartments(memberships);
            userRepository.save(persistedUser);
            updatedUsers.add(persistedUser.getUsername());
        }

        if (!updatedUsers.isEmpty()) {
            log.info("Bootstrap members added to {}: {}", department.getName(), updatedUsers);
        }
    }
}