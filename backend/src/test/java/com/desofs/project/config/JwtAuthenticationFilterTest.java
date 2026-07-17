package com.desofs.project.config;

import com.desofs.project.user.services.ITokenRevocationService;
import com.desofs.project.user.services.IUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private IUserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ITokenRevocationService tokenRevocationService;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(userService, jwtService, tokenRevocationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithoutBearerHeader_ContinuesWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(jwtService, tokenRevocationService, userService);
    }

    @Test
    void doFilterInternal_WithValidToken_AuthenticatesUser() throws ServletException, IOException {
        MockHttpServletRequest request = bearerRequest("jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        UserDetails userDetails = User.withUsername("alice").password("secret").roles("USER").build();

        when(jwtService.parseToken("jwt-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("token-id");
        when(tokenRevocationService.isRevoked("token-id")).thenReturn(false);
        when(claims.getSubject()).thenReturn("alice");
        when(userService.loadUserByUsername("alice")).thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void doFilterInternal_WithRevokedToken_DoesNotAuthenticateUser() throws ServletException, IOException {
        MockHttpServletRequest request = bearerRequest("revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.parseToken("revoked-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("revoked-id");
        when(tokenRevocationService.isRevoked("revoked-id")).thenReturn(true);
        when(claims.getSubject()).thenReturn("alice");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(userService);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ContinuesWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = bearerRequest("bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.parseToken("bad-token")).thenThrow(new JwtException("invalid"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(tokenRevocationService, userService);
    }

    @Test
    void doFilterInternal_WhenContextAlreadyAuthenticated_DoesNotReloadUser() throws ServletException, IOException {
        MockHttpServletRequest request = bearerRequest("jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        TestingAuthenticationToken existingAuthentication = new TestingAuthenticationToken("existing", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        when(jwtService.parseToken("jwt-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("token-id");
        when(tokenRevocationService.isRevoked("token-id")).thenReturn(false);
        when(claims.getSubject()).thenReturn("alice");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuthentication);
        verifyNoInteractions(userService);
        verifyNoMoreInteractions(tokenRevocationService);
    }

    private static MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
