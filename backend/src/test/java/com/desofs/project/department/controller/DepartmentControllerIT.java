package com.desofs.project.department.controller;

import com.desofs.project.config.JwtService;
import com.desofs.project.department.dtos.DepartmentRequest;
import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User manager;
    private User user;

    @BeforeEach
    void setup() {
        departmentRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .username("admin_dep")
                .email("admin_dep@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(Role.ADMIN)
                .build());

        manager = userRepository.save(User.builder()
                .username("manager_dep")
                .email("manager_dep@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(Role.MANAGER)
                .build());

        user = userRepository.save(User.builder()
                .username("user_dep")
                .email("user_dep@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(Role.USER)
                .build());
    }

    @Test
    void user_CanListAndGetDepartments_AndJoinDepartment() throws Exception {
        Department department = departmentRepository.save(Department.builder()
                .name("QA")
                .description("Quality")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(manager))
                .build());

        String userToken = jwtService.generateToken(toUserDetails(user));

        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("QA"));

        mockMvc.perform(get("/api/departments/" + department.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("QA"));

        mockMvc.perform(post("/api/departments/" + department.getId() + "/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(department.getId().toString()));
    }

    @Test
    void user_CanListDepartmentsWithPagination() throws Exception {
        departmentRepository.save(Department.builder()
                .name("Engineering")
                .description("Engineering Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(manager))
                .build());
        departmentRepository.save(Department.builder()
                .name("Operations")
                .description("Operations Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(manager))
                .build());
        departmentRepository.save(Department.builder()
                .name("Compliance")
                .description("Compliance Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(manager))
                .build());

        String userToken = jwtService.generateToken(toUserDetails(user));

        mockMvc.perform(get("/api/departments/directory")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void user_CanFilterDepartmentsByName() throws Exception {
        departmentRepository.save(Department.builder()
                .name("Finance")
                .description("Finance Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(manager))
                .build());
        departmentRepository.save(Department.builder()
                .name("Operations")
                .description("Operations Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(manager))
                .build());

        String userToken = jwtService.generateToken(toUserDetails(user));

        mockMvc.perform(get("/api/departments/directory")
                        .param("name", "fin")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Finance")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void user_CannotUseInvalidDepartmentPaginationParameters() throws Exception {
        String userToken = jwtService.generateToken(toUserDetails(user));

        mockMvc.perform(get("/api/departments/directory")
                        .param("page", "-1")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/departments/directory")
                        .param("page", "0")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void user_CannotUseOversizedDepartmentNameFilter() throws Exception {
        String userToken = jwtService.generateToken(toUserDetails(user));

        mockMvc.perform(get("/api/departments/directory")
                        .param("name", "a".repeat(101))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    @Test
    void manager_CannotCreateDepartmentWithInvalidBody() throws Exception {
        String managerToken = jwtService.generateToken(toUserDetails(manager));
        DepartmentRequest request = new DepartmentRequest("", "a".repeat(1001), Set.of());

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    @Test
    void manager_CannotCreateDepartmentWithBlankDescription() throws Exception {
        String managerToken = jwtService.generateToken(toUserDetails(manager));
        DepartmentRequest request = new DepartmentRequest("Security", " ", Set.of());

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    @Test
    void user_CannotCreateDepartment() throws Exception {
        String userToken = jwtService.generateToken(toUserDetails(user));
        DepartmentRequest request = new DepartmentRequest("Forbidden", "User cannot create departments", Set.of());

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("about:access-denied"))
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void manager_CannotUpdateDepartmentTheyDoNotManage() throws Exception {
        Department department = departmentRepository.save(Department.builder()
                .name("Finance")
                .description("Finance Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(admin))
                .build());

        String managerToken = jwtService.generateToken(toUserDetails(manager));

        DepartmentRequest updateRequest = new DepartmentRequest("Finance", "Updated", Set.of(manager.getId()));

        mockMvc.perform(put("/api/departments/" + department.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_CannotDeleteDepartmentTheyDoNotManage() throws Exception {
        Department department = departmentRepository.save(Department.builder()
                .name("Legal")
                .description("Legal Dept")
                .createdBy(admin.getId())
                .updatedBy(admin.getId())
                .managers(Set.of(admin))
                .build());

        String managerToken = jwtService.generateToken(toUserDetails(manager));

        mockMvc.perform(delete("/api/departments/" + department.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("about:access-denied"))
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
