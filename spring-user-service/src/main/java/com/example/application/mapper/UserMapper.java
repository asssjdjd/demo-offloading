package com.example.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.application.dto.CreateUserRequest;
import com.example.application.dto.UserResponse;
import com.example.domain.model.User;

import java.util.List;

/**
 * MapStruct mapper for User entity <-> DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);
}
