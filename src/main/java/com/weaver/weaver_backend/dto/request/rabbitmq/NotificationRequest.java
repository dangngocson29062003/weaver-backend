package com.weaver.weaver_backend.dto.request.rabbitmq;

import com.weaver.weaver_backend.common.NotificationType;

import java.util.UUID;

public record NotificationRequest(UUID userId, String title, String message, NotificationType type) {
}
