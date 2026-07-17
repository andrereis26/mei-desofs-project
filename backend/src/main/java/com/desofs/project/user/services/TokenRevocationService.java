package com.desofs.project.user.services;

import com.desofs.project.audit.services.IAuditService;
import com.desofs.project.config.JwtService;
import com.desofs.project.user.repositories.TokenRevocationStore;
import com.desofs.project.user.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenRevocationService implements ITokenRevocationService {

    private final TokenRevocationStore tokenRevocationStore;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final IAuditService auditService;

    @Override
    @Transactional
    public void revoke(String token, String username) {
        Claims claims = jwtService.parseToken(token);
        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            throw new BadCredentialsException("Invalid token");
        }
        String subject = claims.getSubject();
        String resolvedUsername = StringUtils.hasText(subject) ? subject : username;
        Instant expiresAt = toInstant(claims.getExpiration());
        tokenRevocationStore.revoke(tokenId, expiresAt, resolvedUsername); // set the TTL based on token expiration
        userRepository.findByUsername(resolvedUsername)
                .ifPresent(user -> auditService.log("REVOKE_TOKEN", "Token", tokenId, user.getId(),
                        "Token revoked for user: " + resolvedUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return true;
        }
        return tokenRevocationStore.isRevoked(tokenId);
    }

    private Instant toInstant(Date date) {
        if (date == null) {
            throw new BadCredentialsException("Invalid token");
        }
        return date.toInstant();
    }
}
