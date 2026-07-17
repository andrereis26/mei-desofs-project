package com.desofs.project.infrastructure.tokenrevocation;

import com.desofs.project.user.repositories.TokenRevocationStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(prefix = "app.token-revocation", name = "store", havingValue = "in-memory")
public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    private final ConcurrentMap<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt, String username) {
        if (!StringUtils.hasText(tokenId) || expiresAt == null) {
            return;
        }
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            revoked.remove(tokenId);
            return;
        }
        revoked.put(tokenId, expiresAt);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return true;
        }
        Instant expiresAt = revoked.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (!expiresAt.isAfter(Instant.now())) {
            revoked.remove(tokenId);
            return false;
        }
        return true;
    }
}
