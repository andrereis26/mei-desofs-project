package com.desofs.project.user.mapper;

import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    UserDto toDto(User user);
}
