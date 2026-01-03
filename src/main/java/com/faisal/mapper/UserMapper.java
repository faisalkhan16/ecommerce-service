package com.faisal.mapper;

import com.faisal.dto.request.CreateUserRequest;
import com.faisal.dto.response.UserResponse;
import com.faisal.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    User fromCreate(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget User user, CreateUserRequest request);
}