package com.desofs.project.user.controller;

import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.user.dtos.UpdateUserRequest;
import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.services.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private IUserService userService;

    @InjectMocks
    private UserController controller;

    @Test
    void getAllUsers_ReturnsUsersFromService() {
        List<UserDto> users = List.of(UserDto.builder().username("alice").build());
        when(userService.findAll()).thenReturn(users);

        assertThat(controller.getAllUsers().getBody()).isEqualTo(users);
    }

    @Test
    void listUserDirectory_BuildsPageRequestAndReturnsPage() {
        PageResponse<UserDto> page = PageResponse.<UserDto>builder().content(List.of()).page(1).size(5).build();
        when(userService.listUsers(eq(Role.USER), org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);

        ResponseEntity<PageResponse<UserDto>> response = controller.listUserDirectory(Role.USER, 1, 5);

        assertThat(response.getBody()).isEqualTo(page);
        verify(userService).listUsers(eq(Role.USER), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void getUserById_AllowsAdminsAndSelfOnly() {
        UUID id = UUID.randomUUID();
        UserDto target = UserDto.builder().id(id).username("alice").build();
        when(userService.findById(id)).thenReturn(target);

        assertThat(controller.getUserById(id, principal("admin", "ROLE_ADMIN")).getBody()).isEqualTo(target);
        assertThat(controller.getUserById(id, principal("alice", "ROLE_USER")).getBody()).isEqualTo(target);
        assertThatThrownBy(() -> controller.getUserById(id, principal("bob", "ROLE_USER")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateUser_AllowsAdminAndDelegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("new@example.com");
        UserDto target = UserDto.builder().id(id).username("alice").build();
        UserDto updated = UserDto.builder().id(id).username("alice").email("new@example.com").build();
        when(userService.findById(id)).thenReturn(target);
        when(userService.update(id, request, "admin")).thenReturn(updated);

        assertThat(controller.updateUser(id, request, principal("admin", "ROLE_ADMIN")).getBody()).isEqualTo(updated);
    }

    private static UserDetails principal(String username, String authority) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded")
                .authorities(authority)
                .build();
    }
}
