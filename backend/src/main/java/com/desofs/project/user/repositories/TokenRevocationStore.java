package com.desofs.project.user.repositories;

import java.time.Instant;

public interface TokenRevocationStore {
    void revoke(String tokenId, Instant expiresAt, String username);
    boolean isRevoked(String tokenId);
}
