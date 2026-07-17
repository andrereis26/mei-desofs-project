package com.desofs.project.user.repositories;

import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
    List<User> findAll();
    Page<User> listUsers(Role role, Pageable pageable);
    Optional<User> findById(UUID id);
    List<User> findAllById(Iterable<UUID> ids);
    void deleteAll();
}
