package com.weaver.weaver_backend.controller;

import com.nimbusds.jose.JOSEException;
import com.weaver.weaver_backend.dto.request.auth.CreateUserRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginRequest;
import com.weaver.weaver_backend.dto.request.auth.PasswordResetRequest;
import com.weaver.weaver_backend.dto.response.ApiResponse;
import com.weaver.weaver_backend.dto.response.auth.CreateUserResponse;
import com.weaver.weaver_backend.dto.response.auth.LoginResponse;
import com.weaver.weaver_backend.service.IAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.text.ParseException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService iAuthService;
    private final SpringTemplateEngine templateEngine;

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, @CookieValue("device_id") String deviceId, HttpServletResponse response) {
        LoginResponse data = iAuthService.login(request, deviceId);
        // REQUIRE 2FA
        if (data.twoFAToken() != null) {
            Cookie mfaCookie =
                    new Cookie(
                            "mfa_token",
                            data.twoFAToken()
                    );
            mfaCookie.setHttpOnly(true);
            mfaCookie.setSecure(false);
            mfaCookie.setPath("/");
            mfaCookie.setMaxAge(5 * 60);
            response.addCookie(mfaCookie);
        } else {
            Cookie refreshCookie =
                    new Cookie(
                            "refresh_token",
                            data.refreshToken()
                    );
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(14 * 24 * 60 * 60);
            response.addCookie(refreshCookie);
        }

        return ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successfully")
                .data(data)
                .build();
    }

    @PostMapping("/2fa")
    ApiResponse<LoginResponse> verifyTwoFA(HttpServletResponse response,
                                           @RequestParam String otp,
                                           @CookieValue("mfa_token") String token,
                                           @CookieValue("device_id") String deviceId) {
        LoginResponse data = iAuthService.verifyTwoFA(token, otp, deviceId);
        Cookie refreshToken = new Cookie("refresh_token", data.refreshToken());
        refreshToken.setHttpOnly(true); // Prevents JavaScript from accessing the cookie (XSS protection)
        refreshToken.setSecure(false); // Change to true in production
        refreshToken.setPath("/"); // Cookie is accessible across all paths in the app
        refreshToken.setMaxAge(14 * 24 * 60 * 60); // Cookie expiry: 14 days — matches refresh token TTL
        response.addCookie(refreshToken);
        Cookie mfaCookie = new Cookie("mfa_token", "");
        mfaCookie.setHttpOnly(true);
        mfaCookie.setSecure(false);
        mfaCookie.setPath("/");
        mfaCookie.setMaxAge(0);
        response.addCookie(mfaCookie);
        return ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successful")
                .data(data)
                .build();
    }

    @PostMapping("/2fa/verify-backup")
    ApiResponse<LoginResponse> verifyBackupCode(HttpServletResponse response,
                                                @RequestParam String backupCode,
                                                @CookieValue("mfa_token") String token,
                                                @CookieValue("device_id") String deviceId) {
        LoginResponse data = iAuthService.verifyBackupCode(token, backupCode, deviceId);
        Cookie refreshToken = new Cookie("refresh_token", data.refreshToken());
        refreshToken.setHttpOnly(true); // Prevents JavaScript from accessing the cookie (XSS protection)
        refreshToken.setSecure(false); // Change to true in production
        refreshToken.setPath("/"); // Cookie is accessible across all paths in the app
        refreshToken.setMaxAge(14 * 24 * 60 * 60); // Cookie expiry: 14 days — matches refresh token TTL
        response.addCookie(refreshToken);
        Cookie mfaCookie = new Cookie("mfa_token", "");
        mfaCookie.setHttpOnly(true);
        mfaCookie.setSecure(false);
        mfaCookie.setPath("/");
        mfaCookie.setMaxAge(0);
        response.addCookie(mfaCookie);
        return ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successful")
                .data(data)
                .build();
    }

    @PostMapping("/register")
    ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        var data = iAuthService.createUser(request);
        return ApiResponse.<CreateUserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse<LoginResponse> verifyEmail(@RequestParam("token") String token,
                                                  @CookieValue("device_id") String deviceId) {
        LoginResponse data = iAuthService.verifyEmail(token, deviceId);
        return ApiResponse.success(data, "Account verified and logged in successfully!");
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) throws ParseException, JOSEException {
        iAuthService.logout(refreshToken);

        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> sendPasswordResetEmail(@Valid
                                                    @NotBlank(message = "Email is required")
                                                    @Email(message = "Invalid email address")
                                                    @RequestParam String email) {

        iAuthService.sendPasswordResetEmail(email);
        return ApiResponse.success(null, "Send password reset email successfully!");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestParam("token") String token,
                                           @Valid @RequestBody PasswordResetRequest request) {
        iAuthService.resetPassword(token, request.password());
        return ApiResponse.success(null, "Reset password successfully!");
    }

    @PostMapping("/refresh-token")
    ApiResponse<LoginResponse> refreshToken(@CookieValue("refresh_token") String refreshToken) {
        var data = iAuthService.refreshToken(refreshToken);

        return ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Token refreshed successfully")
                .data(data)
                .build();
    }
}
