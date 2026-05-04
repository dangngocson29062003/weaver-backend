package com.weaver.weaver_backend.dto.request.email;

import java.util.UUID;

public record EmailRequest(UUID userId, String email) {
}
