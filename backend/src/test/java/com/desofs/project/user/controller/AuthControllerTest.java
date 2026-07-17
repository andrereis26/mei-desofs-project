package com.desofs.project.user.controller;

import com.desofs.project.config.JwtService;
import com.desofs.project.shared.ratelimit.AuthRateLimitGuard;
import com.desofs.project.user.dtos.AuthResponse;
import com.desofs.project.user.dtos.LoginRequest;
import com.desofs.project.user.dtos.RegisterRequest;
import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.services.ITokenRevocationService;
import com.desofs.project.user.services.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private IUserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthRateLimitGuard authRateLimitGuard;

    @Mock
    private ITokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthController controller;

    @Test
    void login_WhenCredentialsAreValid_ReturnsTokenResponse() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("password");
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("encoded")
                .authorities("ROLE_ADMIN")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("jwt");
        when(jwtService.getExpiration()).thenReturn(60000L);

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt");
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
        verify(authRateLimitGuard).checkLoginIdentityLimit("alice");
    }

    @Test
    void register_WhenRequestIsValid_ReturnsCreatedTokenResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password");
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .role("USER")
                .build();
        when(userService.register(request)).thenReturn(user);
        when(jwtService.generateToken("alice")).thenReturn("jwt");
        when(jwtService.getExpiration()).thenReturn(60000L);

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("alice");
        assertThat(response.getBody().getRole()).isEqualTo("USER");
        verify(authRateLimitGuard).checkRegisterUsernameLimit("alice");
        verify(authRateLimitGuard).checkRegisterEmailLimit("alice@example.com");
    }

    @Test
    void logout_WhenBearerTokenIsValid_RevokesToken() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();

        ResponseEntity<Void> response = controller.logout("Bearer token", principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(tokenRevocationService).revoke("token", "alice");
    }

    @Test
    void logout_WhenAuthorizationHeaderIsInvalid_ThrowsBadCredentials() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();

        assertThatThrownBy(() -> controller.logout("token", principal))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> controller.logout(null, principal))
                .isInstanceOf(BadCredentialsException.class);
    }
}
