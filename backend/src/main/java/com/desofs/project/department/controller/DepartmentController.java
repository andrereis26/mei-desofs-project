package com.desofs.project.department.controller;

import com.desofs.project.department.dtos.DepartmentDto;
import com.desofs.project.department.dtos.DepartmentRequest;
import com.desofs.project.department.services.IDepartmentService;
import com.desofs.project.shared.dtos.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final IDepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentDto> create(@Valid @RequestBody DepartmentRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> list() {
        return ResponseEntity.ok(departmentService.listAll());
    }

    @GetMapping("/directory")
    public ResponseEntity<PageResponse<DepartmentDto>> list(
            @RequestParam(required = false) @Size(max = 100, message = "name must be less than or equal to 100 characters") String name,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be greater than or equal to 1")
            @Max(value = 100, message = "size must be less than or equal to 100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(departmentService.listDepartments(name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.findById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<DepartmentDto> findByName(
            @PathVariable @Size(max = 100, message = "name must be less than or equal to 100 characters") String name) {
        return ResponseEntity.ok(departmentService.findByName(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> update(@PathVariable UUID id,
                                                @Valid @RequestBody DepartmentRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(departmentService.update(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        departmentService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<DepartmentDto> joinDepartment(@PathVariable UUID id,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(departmentService.joinDepartment(id, userDetails.getUsername()));
    }
}
