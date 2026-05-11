package com.weaver.weaver_backend.mapper;

import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.entity.Notification;
import com.weaver.weaver_backend.entity.NotificationUser;

public class NotificationMapper {

    private NotificationMapper() {

    }

    public static NotificationResponse toNotificationResponse(NotificationUser notificationUser) {
        Notification notification = notificationUser.getNotification();
        return NotificationResponse.builder()
                .createdAt(notification.getCreatedAt())
                .id(notificationUser.getId())
                .title(notification.getTitle())
                .isRead(notificationUser.isRead())
                .message(notification.getMessage())
                .build();
    }
}
