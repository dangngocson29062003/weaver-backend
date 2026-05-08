package com.weaver.weaver_backend.controller;

import com.weaver.weaver_backend.dto.response.ApiResponse;
import com.weaver.weaver_backend.dto.response.TwoFAResponse;
import com.weaver.weaver_backend.dto.response.auth.AuthUserResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final IUserService iUserService;

    @GetMapping("/me")
    ApiResponse<UserDetailResponse> getMe(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        UserDetailResponse data = iUserService.getMe(authUserResponse.id());
        return ApiResponse.success(data, "User info retrieved successfully");
    }

    @GetMapping("/notifications")
    ApiResponse<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        List<NotificationResponse> data = iUserService.getNotifications(authUserResponse.id());
        return ApiResponse.success(data, "Get notifications successfully");
    }

    @GetMapping("/2fa/setup")
    ApiResponse<TwoFAResponse> setupTwoFA(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        TwoFAResponse data = iUserService.setupTwoFA(authUserResponse.id());
        return ApiResponse.success(data, "The 2FA set up successfully");
    }

    @PostMapping("/2fa")
    ApiResponse<UserDetailResponse> enable2FA(@AuthenticationPrincipal AuthUserResponse authUserResponse, @RequestParam String otp) {
        UserDetailResponse data = iUserService.toggle2FA(authUserResponse.id(), otp);
        return ApiResponse.success(data, data.twoFaEnabled() ? "The 2FA enabled successfully" : "The 2FA disabled successfully");
    }

}
