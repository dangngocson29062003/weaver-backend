package com.weaver.weaver_backend.dto.request.user;

import java.util.List;

public record NotificationMarkRequest(
        List<Long> notificationUserIds
) {
}
