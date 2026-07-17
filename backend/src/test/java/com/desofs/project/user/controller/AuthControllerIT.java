package com.desofs.project.user.controller;

import com.desofs.project.user.dtos.LoginRequest;
import com.desofs.project.user.dtos.RegisterRequest;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

    @Test
    void register_WithValidData_Returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integrationuser");
        request.setEmail("integration@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("integrationuser"));
    }

        @Test
        void register_WithInvalidData_Returns400() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setUsername("in");
                request.setEmail("invalid-email");
                request.setPassword("short");

                mockMvc.perform(post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.type").value("about:validation-error"));
        }

    @Test
    void logout_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_WithValidToken_Returns204() throws Exception {
        long suffix = System.currentTimeMillis();

        RegisterRequest request = new RegisterRequest();
        request.setUsername("logoutuser" + suffix);
        request.setEmail("logout" + suffix + "@example.com");
        request.setPassword("password123");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("token")
                .asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void revokedToken_IsRejectedOnProtectedEndpoint() throws Exception {
        long suffix = System.currentTimeMillis();

        RegisterRequest request = new RegisterRequest();
        request.setUsername("revokeduser" + suffix);
        request.setEmail("revoked" + suffix + "@example.com");
        request.setPassword("password123");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("token")
                .asText();

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

        @Test
        void login_WithMissingPassword_Returns400() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setUsername("missingpass");
                request.setPassword("");

                mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.type").value("about:validation-error"));
        }

        @Test
        void login_WithWrongPassword_Returns401AndSafeMessage() throws Exception {
                long suffix = System.currentTimeMillis();

                RegisterRequest request = new RegisterRequest();
                request.setUsername("loginuser" + suffix);
                request.setEmail("login" + suffix + "@example.com");
                request.setPassword("password123");

                mockMvc.perform(post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername(request.getUsername());
                loginRequest.setPassword("wrongpassword");

                mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.type").value("about:authentication-failed"))
                                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
        }
}
