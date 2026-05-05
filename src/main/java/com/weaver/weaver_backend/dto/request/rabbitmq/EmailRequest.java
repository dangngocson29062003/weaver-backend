package com.weaver.weaver_backend.dto.request.rabbitmq;

import java.util.UUID;

public record EmailRequest(UUID userId, String email) {
}
