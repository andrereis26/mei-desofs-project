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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IAuditService auditService;

    @Mock
    private UserDtoMapper userDtoMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails result = userService.loadUserByUsername("testuser");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void loadUserByUsername_WhenUserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void register_WhenValidRequest_ReturnsUserDto() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        UserDto mapped = UserDto.builder().id(testUserId).username("testuser").email("test@example.com").role("USER").build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDtoMapper.toDto(testUser)).thenReturn(mapped);
        when(auditService.log(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(null);

        UserDto result = userService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
        verify(auditService).log(eq("REGISTER"), eq("User"), eq(testUserId.toString()), eq(testUserId), contains("testuser"));
    }

    @Test
    void register_WhenUsernameExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("other@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RegistrationException.class);
    }

    @Test
    void register_WhenEmailExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RegistrationException.class);
    }

    @Test
    void findAll_ReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(userDtoMapper.toDto(testUser)).thenReturn(UserDto.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build());

        List<UserDto> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void listUsers_WhenUsersExist_ReturnsPagedResponse() {
        Pageable pageable = PageRequest.of(1, 2);
        UserDto mapped = UserDto.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();

        when(userRepository.listUsers(eq(Role.USER), same(pageable)))
                .thenReturn(new PageImpl<>(List.of(testUser), pageable, 5));
        when(userDtoMapper.toDto(testUser)).thenReturn(mapped);

        PageResponse<UserDto> result = userService.listUsers(Role.USER, pageable);

        assertThat(result.getContent()).containsExactly(mapped);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        verify(userRepository).listUsers(eq(Role.USER), same(pageable));
        verify(userDtoMapper).toDto(testUser);
    }

    @Test
    void listUsers_WhenNoUsersExist_ReturnsEmptyPagedResponse() {
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.listUsers(isNull(), same(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<UserDto> result = userService.listUsers(null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        verify(userRepository).listUsers(isNull(), same(pageable));
        verifyNoInteractions(userDtoMapper);
    }

    @Test
    void findById_WhenUserExists_ReturnsDto() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userDtoMapper.toDto(testUser)).thenReturn(UserDto.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build());

        UserDto result = userService.findById(testUserId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
    }

    @Test
    void findById_WhenUserNotFound_ThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(unknownId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void update_WhenAdminUpdatesAnotherUserEmail_SavesAndAuditsChange() {
        User admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        UpdateUserRequest request = new UpdateUserRequest("updated@example.com");
        UserDto mapped = UserDto.builder()
                .id(testUserId)
                .username("testuser")
                .email("updated@example.com")
                .role("USER")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userDtoMapper.toDto(testUser)).thenReturn(mapped);

        UserDto result = userService.update(testUserId, request, "admin");

        assertThat(testUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        verify(auditService).log(eq("UPDATE"), eq("User"), eq(testUserId.toString()), eq(admin.getId()),
                contains("admin"));
    }

    @Test
    void update_WhenUserUpdatesOwnProfileWithSameEmail_SkipsUniquenessCheck() {
        UpdateUserRequest request = new UpdateUserRequest("test@example.com");
        UserDto mapped = UserDto.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userDtoMapper.toDto(testUser)).thenReturn(mapped);

        UserDto result = userService.update(testUserId, request, "testuser");

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void update_WhenNonAdminUpdatesAnotherUser_ThrowsSecurityException() {
        UUID otherUserId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("updated@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.update(otherUserId, request, "testuser"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("cannot modify another user's profile");
    }

    @Test
    void update_WhenEmailAlreadyExists_RejectsChange() {
        UpdateUserRequest request = new UpdateUserRequest("taken@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(testUserId, request, "testuser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");
    }
}
