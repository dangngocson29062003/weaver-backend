package com.weaver.weaver_backend.dto.response.user;

import com.weaver.weaver_backend.common.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String title, String message, Boolean isRead, NotificationType type, Instant createdAt) {
}
