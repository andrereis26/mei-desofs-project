package com.desofs.project.user.controller;

import com.desofs.project.config.JwtService;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.document.repositories.DocumentRepository;
import com.desofs.project.user.dtos.UpdateUserRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JwtService jwtService;

    private User user;
    private User admin;
    private User manager;

    @BeforeEach
    void setup() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .username("admin_user")
                .email("admin_user@example.com")
                .password("password123")
                .role(Role.ADMIN)
                .build());
        manager = userRepository.save(User.builder()
                .username("manager_user")
                .email("manager_user@example.com")
                .password("password123")
                .role(Role.MANAGER)
                .build());
        user = userRepository.save(User.builder()
                .username("basic_user")
                .email("basic_user@example.com")
                .password("raccoon123")
                .role(Role.USER)
                .build());
    }

    @Test
    void user_CannotListAllUsers() throws Exception {
        String userToken = jwtService.generateToken(toUserDetails(user));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_CannotListUserDirectory() throws Exception {
        String managerToken = jwtService.generateToken(toUserDetails(manager));

        mockMvc.perform(get("/api/users/directory")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_CanListAllUsersWithoutPagination() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void admin_CanListUsersWithPagination() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(get("/api/users/directory")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void admin_CanFilterUsersByRole() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(get("/api/users/directory")
                        .param("role", "MANAGER")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("manager_user")))
                .andExpect(jsonPath("$.content[0].role", is("MANAGER")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void admin_CannotUseInvalidPaginationParameters() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(get("/api/users/directory")
                        .param("page", "-1")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users/directory")
                        .param("page", "0")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_CannotUseInvalidRoleFilter() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(get("/api/users/directory")
                        .param("role", "OWNER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_CannotUpdateUserWithInvalidEmail() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));
        UpdateUserRequest request = new UpdateUserRequest("invalid-email");

        mockMvc.perform(put("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    @Test
    void user_CannotUpdateAnotherUser() throws Exception {
        String userToken = jwtService.generateToken(toUserDetails(user));
        UpdateUserRequest request = new UpdateUserRequest("manager_new@example.com");

        mockMvc.perform(put("/api/users/" + manager.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_CannotUpdateUserWithBlankEmail() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));
        UpdateUserRequest request = new UpdateUserRequest(" ");

        mockMvc.perform(put("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    @Test
    void admin_CannotUpdateUserWithoutEmail() throws Exception {
        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(put("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
