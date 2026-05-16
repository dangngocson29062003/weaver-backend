package com.weaver.weaver_backend.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weaver.weaver_backend.common.NotificationType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(Long id, String title, String message, Boolean isRead, NotificationType type, LocalDateTime createdAt,String actionUrl) {
}
