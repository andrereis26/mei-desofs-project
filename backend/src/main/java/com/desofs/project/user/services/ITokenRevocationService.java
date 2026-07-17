package com.desofs.project.user.services;

public interface ITokenRevocationService {
    void revoke(String token, String username);
    boolean isRevoked(String tokenId);
}
