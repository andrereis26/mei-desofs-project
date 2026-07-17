package com.desofs.project.department.mapper;

import com.desofs.project.department.dtos.DepartmentDto;
import com.desofs.project.department.dtos.DepartmentUserDto;
import com.desofs.project.department.model.Department;
import com.desofs.project.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DepartmentDtoMapper {

    @Mapping(target = "managers", source = "department.managers", qualifiedByName = "usersToSummaries")
    @Mapping(target = "members", source = "members", qualifiedByName = "usersToSummaries")
    DepartmentDto toDto(Department department, Set<User> members);

    default DepartmentDto toDto(Department department) {
        return toDto(department, Set.of());
    }

    @Named("usersToSummaries")
    default Set<DepartmentUserDto> usersToSummaries(Set<User> users) {
        if (users == null) {
            return Set.of();
        }
        return users.stream()
                .map(user -> DepartmentUserDto.builder().id(user.getId()).username(user.getUsername()).build())
                .collect(Collectors.toSet());
    }
}
