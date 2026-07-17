package com.desofs.project.user.services;

import com.desofs.project.audit.services.IAuditService;
import com.desofs.project.config.JwtService;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.TokenRevocationStore;
import com.desofs.project.user.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenRevocationServiceTest {

    private final TokenRevocationStore store = mock(TokenRevocationStore.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final IAuditService auditService = mock(IAuditService.class);
    private final TokenRevocationService service = new TokenRevocationService(
            store, jwtService, userRepository, auditService);

    @Test
    void revoke_WithValidClaims_RevokesTokenAndAuditsResolvedSubject() {
        Claims claims = claims("token-1", "alice", Date.from(Instant.now().plusSeconds(60)));
        User user = User.builder().id(UUID.randomUUID()).username("alice").role(Role.USER).build();
        when(jwtService.parseToken("jwt")).thenReturn(claims);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        service.revoke("jwt", "fallback");

        verify(store).revoke(eq("token-1"), any(Instant.class), eq("alice"));
        verify(auditService).log(eq("REVOKE_TOKEN"), eq("Token"), eq("token-1"), eq(user.getId()),
                eq("Token revoked for user: alice"));
    }

    @Test
    void revoke_WhenSubjectIsBlank_UsesFallbackUsernameWithoutAuditWhenUserMissing() {
        Claims claims = claims("token-2", " ", Date.from(Instant.now().plusSeconds(60)));
        when(jwtService.parseToken("jwt")).thenReturn(claims);
        when(userRepository.findByUsername("fallback")).thenReturn(Optional.empty());

        service.revoke("jwt", "fallback");

        verify(store).revoke(eq("token-2"), any(Instant.class), eq("fallback"));
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void revoke_WhenTokenIdOrExpirationMissing_RejectsToken() {
        Claims missingIdClaims = claims(" ", "alice", Date.from(Instant.now().plusSeconds(60)));
        Claims missingExpirationClaims = claims("token-3", "alice", null);

        assertThatThrownBy(() -> {
            when(jwtService.parseToken("missing-id")).thenReturn(missingIdClaims);
            service.revoke("missing-id", "alice");
        }).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> {
            when(jwtService.parseToken("missing-exp")).thenReturn(missingExpirationClaims);
            service.revoke("missing-exp", "alice");
        }).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void isRevoked_BlankTokenIdsAreAlwaysRevoked() {
        when(store.isRevoked("token-1")).thenReturn(true);

        assertThat(service.isRevoked(null)).isTrue();
        assertThat(service.isRevoked(" ")).isTrue();
        assertThat(service.isRevoked("token-1")).isTrue();
    }

    private static Claims claims(String id, String subject, Date expiration) {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn(id);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.getExpiration()).thenReturn(expiration);
        return claims;
    }
}
