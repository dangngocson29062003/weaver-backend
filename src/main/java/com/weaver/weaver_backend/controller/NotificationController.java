package com.weaver.weaver_backend.controller;

import com.weaver.weaver_backend.dto.request.user.NotificationMarkRequest;
import com.weaver.weaver_backend.dto.request.user.UnreadCountResponse;
import com.weaver.weaver_backend.dto.response.ApiResponse;
import com.weaver.weaver_backend.dto.response.PageResponse;
import com.weaver.weaver_backend.dto.response.auth.AuthUserResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.service.INotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService iNotificationService;

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markSingleAsRead(@PathVariable long id, @AuthenticationPrincipal AuthUserResponse authUserResponse) {
        var data = iNotificationService.markSingleAsRead(authUserResponse.id(),id);
        return ApiResponse.success(data, "Mark Single Notification As Read Successfully");
    }

    @PutMapping("/read")
    public ApiResponse<List<NotificationResponse>> markAllAsRead(@RequestBody NotificationMarkRequest request, @AuthenticationPrincipal AuthUserResponse authUserResponse) {
        var data = iNotificationService.markAllAsRead(authUserResponse.id(), request);
        return ApiResponse.success(data, "Mark All Notifications As Read Successfully");
    }


    @GetMapping("/notifications")
    ApiResponse<PageResponse<NotificationResponse>> getNotifications(@AuthenticationPrincipal AuthUserResponse authUserResponse,
                                                                     @RequestParam(required = false, defaultValue = "1") int page,
                                                                     @RequestParam(required = false, defaultValue = "5") int size) {
        PageResponse<NotificationResponse> data = iNotificationService.findAllByUserId(authUserResponse.id(),page,size);
        return ApiResponse.success(data, "User's Notifications retrieved successfully");
    }

    @GetMapping("/notifcations/unread")
    ApiResponse<UnreadCountResponse> getUnreadNotifications(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        var data = iNotificationService.getUnreadNotificationQuantity(authUserResponse.id());
        return ApiResponse.success(data, "Unread notifications quantity retrieved successfully");
    }
}
