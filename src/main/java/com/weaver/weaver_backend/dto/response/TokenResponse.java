package com.weaver.weaver_backend.dto.response;

import lombok.Builder;

@Builder
public record TokenResponse(
        String value,      // Token string (JWT)
        String jwtId,      // UUID của token
        long ttlSeconds    // TTL tính bằng giây
) {}
