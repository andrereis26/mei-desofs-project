package com.desofs.project.user.services;

import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.user.dtos.RegisterRequest;
import com.desofs.project.user.dtos.UpdateUserRequest;
import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.model.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.UUID;

public interface IUserService extends UserDetailsService {
    UserDto register(RegisterRequest request);
    List<UserDto> findAll();
    PageResponse<UserDto> listUsers(Role role, Pageable pageable);
    UserDto findById(UUID id);
    UserDto update(UUID id, UpdateUserRequest request, String currentUsername);
}
