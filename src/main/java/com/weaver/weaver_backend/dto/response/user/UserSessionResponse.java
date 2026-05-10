package com.weaver.weaver_backend.dto.response.user;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserSessionResponse(
        UUID id,
        String os,
        String browser,
        String deviceType,
        String ipAddress,
        Instant lastActive,
        boolean isCurrent,
        boolean isExpired
) {
}