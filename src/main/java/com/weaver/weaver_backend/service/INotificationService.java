package com.weaver.weaver_backend.service;

import com.weaver.weaver_backend.dto.request.user.NotificationMarkRequest;
import com.weaver.weaver_backend.dto.request.user.UnreadCountResponse;
import com.weaver.weaver_backend.dto.response.PageResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface INotificationService {
    PageResponse<NotificationResponse> findAllByUserId(UUID userId, int page, int size);

    NotificationResponse markSingleAsRead(UUID userId, long notificationId);

    List<NotificationResponse> markAllAsRead(UUID userId, NotificationMarkRequest request);

    UnreadCountResponse getUnreadNotificationQuantity(UUID userId);
}
