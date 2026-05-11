package com.weaver.weaver_backend.dto.request.rabbitmq;

import com.weaver.weaver_backend.common.NotificationType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationRequest(UUID userId, String title, String message, NotificationType type, String actionUrl) {
}
