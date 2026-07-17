package com.desofs.project.department.controller;

import com.desofs.project.department.dtos.DepartmentDto;
import com.desofs.project.department.dtos.DepartmentRequest;
import com.desofs.project.department.services.IDepartmentService;
import com.desofs.project.shared.dtos.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock
    private IDepartmentService departmentService;

    @InjectMocks
    private DepartmentController controller;

    @Test
    void create_ReturnsCreatedDepartment() {
        DepartmentRequest request = new DepartmentRequest("Engineering", "Eng", null);
        DepartmentDto dto = DepartmentDto.builder().id(UUID.randomUUID()).name("Engineering").build();
        when(departmentService.create(request, "alice")).thenReturn(dto);

        var response = controller.create(request, principal("alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void listEndpoints_DelegateToService() {
        UUID id = UUID.randomUUID();
        DepartmentDto dto = DepartmentDto.builder().id(id).name("Engineering").build();
        PageResponse<DepartmentDto> page = PageResponse.<DepartmentDto>builder().content(List.of(dto)).build();
        when(departmentService.listAll()).thenReturn(List.of(dto));
        when(departmentService.listDepartments(eq("Eng"), any(Pageable.class))).thenReturn(page);
        when(departmentService.findById(id)).thenReturn(dto);
        when(departmentService.findByName("Engineering")).thenReturn(dto);

        assertThat(controller.list().getBody()).containsExactly(dto);
        assertThat(controller.list("Eng", 0, 10).getBody()).isEqualTo(page);
        assertThat(controller.findById(id).getBody()).isEqualTo(dto);
        assertThat(controller.findByName("Engineering").getBody()).isEqualTo(dto);
    }

    @Test
    void updateDeleteAndJoin_DelegateToService() {
        UUID id = UUID.randomUUID();
        DepartmentRequest request = new DepartmentRequest("Platform", "Platform", null);
        DepartmentDto dto = DepartmentDto.builder().id(id).name("Platform").build();
        when(departmentService.update(id, request, "alice")).thenReturn(dto);
        when(departmentService.joinDepartment(id, "alice")).thenReturn(dto);

        assertThat(controller.update(id, request, principal("alice")).getBody()).isEqualTo(dto);
        assertThat(controller.delete(id, principal("alice")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.joinDepartment(id, principal("alice")).getBody()).isEqualTo(dto);
        verify(departmentService).delete(id, "alice");
    }

    private static UserDetails principal(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded")
                .authorities("ROLE_USER")
                .build();
    }
}
