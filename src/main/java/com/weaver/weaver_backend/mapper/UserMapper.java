package com.weaver.weaver_backend.mapper;

import com.weaver.weaver_backend.dto.request.auth.CreateUserRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginViaOAuthRequest;
import com.weaver.weaver_backend.dto.response.auth.CreateUserResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    @Mapping(target = "password", ignore = true)
    User toUser(CreateUserRequest request);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "userStatus", constant = "ACTIVE")
    User toUserFromOAuth(LoginViaOAuthRequest request);

    CreateUserResponse toCreateUserResponse(User user);

    UserDetailResponse toUserDetailResponse(User user);
}
