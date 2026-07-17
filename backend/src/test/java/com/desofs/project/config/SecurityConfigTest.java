package com.desofs.project.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final JwtAuthenticationFilter jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final SecurityConfig securityConfig = new SecurityConfig(
            jwtAuthenticationFilter,
            requestIdFilter,
            userDetailsService,
            passwordEncoder
    );

    @Test
    void unauthorizedEntryPoint_SendsUnauthorizedError() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.unauthorizedEntryPoint().commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad credentials")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("bad credentials");
    }

    @Test
    void authenticationProvider_AuthenticatesWithConfiguredUserDetailsAndPasswordEncoder() {
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("encoded").roles("USER").build());
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);

        AuthenticationProvider provider = securityConfig.authenticationProvider();
        Authentication authentication = provider.authenticate(
                new UsernamePasswordAuthenticationToken("alice", "raw")
        );

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("alice");
    }

    @Test
    void authenticationManager_DelegatesToAuthenticationConfiguration() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertThat(securityConfig.authenticationManager(authenticationConfiguration)).isSameAs(authenticationManager);
    }
}
