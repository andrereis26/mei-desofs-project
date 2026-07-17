package com.desofs.project.config;

import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupDataBootstrapTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final StartupDataBootstrap bootstrap = new StartupDataBootstrap(
            userRepository, departmentRepository, passwordEncoder);

    private final Map<String, User> usersByUsername = new HashMap<>();
    private final Map<String, Department> departmentsByName = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("ChangeMe123!")).thenReturn("encoded");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByUsername(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(usersByUsername.get(invocation.getArgument(0))));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenAnswer(invocation -> List.copyOf(usersByUsername.values()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);

            User saved = User.builder()
                    .id(user.getId() == null ? UUID.randomUUID() : user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .departments(user.getDepartments())
                    .build();

            usersByUsername.put(saved.getUsername(), saved);
            return saved;
        });

        when(departmentRepository.findByName(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(departmentsByName.get(invocation.getArgument(0))));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);

            Department saved = Department.builder()
                    .id(department.getId() == null ? UUID.randomUUID() : department.getId())
                    .name(department.getName())
                    .description(department.getDescription())
                    .managers(department.getManagers())
                    .createdBy(department.getCreatedBy())
                    .updatedBy(department.getUpdatedBy())
                    .createdAt(department.getCreatedAt())
                    .updatedAt(department.getUpdatedAt())
                    .build();

            departmentsByName.put(saved.getName(), saved);
            return saved;
        });
    }

    @Test
    void run_WhenSeedDataIsMissing_CreatesUsersDepartmentsManagersAndMembers() {
        bootstrap.run(mock(ApplicationArguments.class));

        assertThat(usersByUsername).containsKeys(
                "bootstrap_admin",
                "bootstrap_manager",
                "bootstrap_manager_2",
                "bootstrap_manager_3",
                "bootstrap_user",
                "bootstrap_user_2",
                "bootstrap_user_3",
                "bootstrap_user_4");
        assertThat(departmentsByName).containsKeys("Engineering", "Operations", "Compliance");
        assertThat(departmentsByName.get("Engineering").getManagers()).hasSize(3);
        assertThat(usersByUsername.get("bootstrap_user").getDepartments())
                .extracting(Department::getId)
                .contains(departmentsByName.get("Engineering").getId());
        verify(passwordEncoder, atLeast(8)).encode("ChangeMe123!");
        verify(departmentRepository, atLeast(6)).save(any(Department.class));
    }

    @Test
    void run_WhenBootstrapAdminCannotBeResolved_SkipsDepartments() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of());

        bootstrap.run(mock(ApplicationArguments.class));

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void run_WhenUserExistsByEmail_UsesExistingUser() {
        User existingAdmin = User.builder()
                .id(UUID.randomUUID())
                .username("existing-admin")
                .email("bootstrap_admin@desofs.local")
                .role(Role.ADMIN)
                .build();
        usersByUsername.put(existingAdmin.getUsername(), existingAdmin);
        when(userRepository.existsByUsername("bootstrap_admin")).thenReturn(false);
        when(userRepository.existsByEmail("bootstrap_admin@desofs.local")).thenReturn(true);
        when(userRepository.findByEmail("bootstrap_admin@desofs.local")).thenReturn(Optional.of(existingAdmin));

        bootstrap.run(mock(ApplicationArguments.class));

        ArgumentCaptor<User> savedUsers = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeast(1)).save(savedUsers.capture());
        assertThat(savedUsers.getAllValues()).noneMatch(user -> "bootstrap_admin".equals(user.getUsername()));
    }
}
