package com.desofs.project.user.controller;

import com.desofs.project.config.RateLimitTestCacheConfiguration;
import com.desofs.project.user.dtos.LoginRequest;
import com.desofs.project.user.dtos.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RateLimitTestCacheConfiguration.class)
@TestPropertySource(properties = {
        "AUTH_RATE_LIMIT_MAX_PER_IP=2",
        "AUTH_RATE_LIMIT_MAX_PER_IDENTITY=2",
        "AUTH_RATE_LIMIT_WINDOW_SECONDS=60",
        "REGISTER_RATE_LIMIT_MAX_PER_IP=2",
        "REGISTER_RATE_LIMIT_MAX_PER_IDENTITY=2",
        "REGISTER_RATE_LIMIT_WINDOW_SECONDS=60"
})
class AuthRateLimitIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_WhenRateLimitExceeded_Returns429() throws Exception {
        String ip = "10.0.0.5";

        RegisterRequest first = registerRequest("rluser1", "rluser1@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = registerRequest("rluser2", "rluser2@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        RegisterRequest third = registerRequest("rluser3", "rluser3@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(third)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("about:rate-limit"))
                .andExpect(jsonPath("$.detail").value("Too many requests"));
    }

    @Test
    void login_WhenRateLimitExceeded_Returns429() throws Exception {
        String ip = "10.0.0.6";
        String suffix = String.valueOf(System.nanoTime());
        RegisterRequest request = registerRequest("loginrl" + suffix, "loginrl" + suffix + "@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUsername());
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("about:rate-limit"))
                .andExpect(jsonPath("$.detail").value("Too many requests"));
    }

    @Test
    void login_WhenIdentityRateLimitExceededAcrossIps_Returns429() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        RegisterRequest request = registerRequest("identityrl" + suffix, "identityrl" + suffix + "@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr("10.0.0.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUsername());
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr("10.0.0.21"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr("10.0.0.22"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr("10.0.0.23"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("about:rate-limit"))
                .andExpect(jsonPath("$.detail").value("Too many requests"));
    }

    @Test
    void register_WhenEmailIdentityRateLimitExceededAcrossIps_Returns429() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "registerrl" + suffix + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr("10.0.0.30"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("registerrl1" + suffix, email))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr("10.0.0.31"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("registerrl2" + suffix, email))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr("10.0.0.32"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("registerrl3" + suffix, email))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("about:rate-limit"))
                .andExpect(jsonPath("$.detail").value("Too many requests"));
    }

    private RegisterRequest registerRequest(String username, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("password123");
        return request;
    }

    private RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
