package com.weaver.weaver_backend.mapper;

import com.weaver.weaver_backend.dto.response.user.UserSessionResponse;
import com.weaver.weaver_backend.entity.UserSession;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserSessionMapper {

    @Mapping(target = "isCurrent", expression = "java(checkIsCurrent(session, currentSessionId))")
    @Mapping(target = "isExpired", expression = "java(checkIsExpired(session))")
    UserSessionResponse toResponse(UserSession session, @Context UUID currentSessionId);

    default boolean checkIsCurrent(UserSession session, UUID currentSessionId) {
        if (session == null || currentSessionId == null) return false;
        return session.getId().equals(currentSessionId);
    }

    default boolean checkIsExpired(UserSession session) {
        if (session == null) return true;
        return (session.getIsRevoked() != null && session.getIsRevoked())
                || (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now()));
    }
}
