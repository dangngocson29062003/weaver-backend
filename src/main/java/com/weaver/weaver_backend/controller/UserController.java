package com.weaver.weaver_backend.controller;

import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.ApiResponse;
import com.weaver.weaver_backend.dto.response.auth.AuthUserResponse;
import com.weaver.weaver_backend.dto.response.user.TwoFASetupResponse;
import com.weaver.weaver_backend.dto.response.user.TwoFAStatusResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.dto.response.user.UserSessionResponse;
import com.weaver.weaver_backend.service.INotificationService;
import com.weaver.weaver_backend.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/sessions")
    ApiResponse<List<UserSessionResponse>> getSessions(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        List<UserSessionResponse> data = iUserService.getSessions(authUserResponse.id(), authUserResponse.sessionId());
        return ApiResponse.success(data, "Get sessions successfully");
    }

    @PostMapping("/sessions/revoke")
    ApiResponse<Void> revokeSession(@AuthenticationPrincipal AuthUserResponse authUserResponse, @RequestParam UUID sessionId) {
        iUserService.revokeSession(authUserResponse.id(), sessionId);
        return ApiResponse.success(null, "Revoke sessions successfully");
    }

    @PostMapping("/sessions/trust")
    ApiResponse<Boolean> trustDevice(@AuthenticationPrincipal AuthUserResponse authUserResponse, @RequestParam UUID sessionId, @RequestParam String otp) {
        boolean isTrusted = iUserService.toggleTrustDevice(authUserResponse.id(), sessionId, otp);
        return ApiResponse.success(isTrusted, isTrusted ? "Device trust activated" : "Device trust revoked");
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
