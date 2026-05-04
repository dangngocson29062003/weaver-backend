package com.weaver.weaver_backend.controller;

import com.nimbusds.jose.JOSEException;
import com.weaver.weaver_backend.dto.response.ApiResponse;
import com.weaver.weaver_backend.dto.response.TwoFAResponse;
import com.weaver.weaver_backend.dto.response.auth.AuthUserResponse;
import com.weaver.weaver_backend.dto.response.auth.UserDetailResponse;
import com.weaver.weaver_backend.service.IUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

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

    @GetMapping("/2fa/setup")
    ApiResponse<TwoFAResponse> setupTwoFA(@AuthenticationPrincipal AuthUserResponse authUserResponse) {
        TwoFAResponse data = iUserService.setupTwoFA(authUserResponse.id());
        return ApiResponse.success(data, "The 2FA set up successfully");
    }

    @PostMapping("/2fa")
    ApiResponse<UserDetailResponse> enable2FA(@AuthenticationPrincipal AuthUserResponse authUserResponse, @RequestParam int OTP) {
        UserDetailResponse data = iUserService.toggle2FA(authUserResponse.id(), OTP);
        return ApiResponse.success(data, data.twoFaEnabled() ? "The 2FA enabled successfully" : "The 2FA disabled successfully");
    }

}
