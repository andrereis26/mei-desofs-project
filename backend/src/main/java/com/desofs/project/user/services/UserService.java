package com.desofs.project.user.services;

import com.desofs.project.audit.services.IAuditService;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.shared.exceptions.RegistrationException;
import com.desofs.project.shared.exceptions.UserNotFoundException;
import com.desofs.project.user.dtos.RegisterRequest;
import com.desofs.project.user.dtos.UpdateUserRequest;
import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.mapper.UserDtoMapper;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAuditService auditService;
    private final UserDtoMapper userDtoMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RegistrationException();
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RegistrationException();
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        auditService.log("REGISTER", "User", saved.getId().toString(), saved.getId(),
                "User registered: " + saved.getUsername());
        return userDtoMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(userDtoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(Role role, Pageable pageable) {
        Page<UserDto> users = userRepository.listUsers(role, pageable)
                .map(userDtoMapper::toDto);

        return PageResponse.from(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
        return userDtoMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(UserNotFoundException::new);

        if (currentUser.getRole() != Role.ADMIN && !currentUser.getId().equals(id)) {
            throw new SecurityException("Access denied: cannot modify another user's profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        User saved = userRepository.save(user);
        auditService.log("UPDATE", "User", saved.getId().toString(), currentUser.getId(),
                "User updated by: " + currentUsername);
        return userDtoMapper.toDto(saved);
    }
}
