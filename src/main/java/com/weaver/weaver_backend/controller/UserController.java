package com.weaver.weaver_backend.controller;

import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.ApiResponse;
import com.weaver.weaver_backend.dto.response.PageResponse;
import com.weaver.weaver_backend.dto.response.user.TwoFASetupResponse;
import com.weaver.weaver_backend.dto.response.auth.AuthUserResponse;
import com.weaver.weaver_backend.dto.response.user.TwoFAStatusResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.service.INotificationService;
import com.weaver.weaver_backend.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final IUserService iUserService;
    private final INotificationService iNotificationService;

    @GetMapping("/me")
    ApiResponse<UserDetailResponse> getMe(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        UserDetailResponse data = iUserService.getMe(authUserResponse.id());
        return ApiResponse.success(data, "User info retrieved successfully");
    }


    @PatchMapping("/password")
    ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthUserResponse authUserResponse, @Valid @RequestBody PasswordRequest request) {
        iUserService.changePassword(authUserResponse.id(), request);
        return ApiResponse.success(null, "Change password successfully");
    }

    @GetMapping("/2fa/setup")
    ApiResponse<TwoFASetupResponse> setupTwoFA(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        TwoFASetupResponse data = iUserService.setupTwoFA(authUserResponse.id());
        return ApiResponse.success(data, "The 2FA set up successfully");
    }

    @PostMapping("/2fa")
    ApiResponse<TwoFAStatusResponse> enable2FA(@AuthenticationPrincipal AuthUserResponse authUserResponse, @RequestParam String otp) {
        TwoFAStatusResponse data = iUserService.toggle2FA(authUserResponse.id(), otp);
        return ApiResponse.success(data, data.enabled() ? "The 2FA enabled successfully" : "The 2FA disabled successfully");
    }

    @PostMapping("/2fa/verify-backup")
    ApiResponse<TwoFAStatusResponse> disable2FAWithBackup(
            @AuthenticationPrincipal AuthUserResponse authUserResponse,
            @RequestParam String backupCode
    ) {
        TwoFAStatusResponse data = iUserService.disable2FAWithBackup(authUserResponse.id(), backupCode);
        return ApiResponse.success(data, "The 2FA disabled successfully using backup code");
    }
}
