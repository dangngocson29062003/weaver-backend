package com.weaver.weaver_backend.dto.response.auth;

import java.util.UUID;

public record AuthUserResponse(UUID id, String email) {
}
