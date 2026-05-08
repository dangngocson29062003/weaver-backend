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
    ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        LoginResponse data = iAuthService.login(request);
        Cookie cookie = new Cookie("refresh_token", data.refreshToken());
        cookie.setHttpOnly(true); // Prevents JavaScript from accessing the cookie (XSS protection)
        cookie.setSecure(false); // Change to true in production
        cookie.setPath("/"); // Cookie is accessible across all paths in the app
        cookie.setMaxAge(14 * 24 * 60 * 60); // Cookie expiry: 14 days — matches refresh token TTL
        response.addCookie(cookie);

        return ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successfully")
                .data(data)
                .build();
    }

    @GetMapping("/2fa")
    ApiResponse<LoginResponse> verifyTwoFA(HttpServletResponse response, @RequestParam String OTP, @RequestHeader("twoFAToken") String token) {
        LoginResponse data = iAuthService.verifyTwoFA(token, OTP);
        Cookie cookie = new Cookie("refresh_token", data.refreshToken());
        cookie.setHttpOnly(true); // Prevents JavaScript from accessing the cookie (XSS protection)
        cookie.setSecure(false); // Change to true in production
        cookie.setPath("/"); // Cookie is accessible across all paths in the app
        cookie.setMaxAge(14 * 24 * 60 * 60); // Cookie expiry: 14 days — matches refresh token TTL
        response.addCookie(cookie);

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
    public ApiResponse<LoginResponse> verifyEmail(@RequestParam("token") String token) {
        LoginResponse data = iAuthService.verifyEmail(token);
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
    public ApiResponse<LoginResponse> sendPasswordResetEmail(@Valid
                                                             @NotBlank(message = "Email is required")
                                                             @Email(message = "Invalid email address")
                                                             @RequestParam String email) {

        iAuthService.sendPasswordResetEmail(email);
        return ApiResponse.success(null, "Send password reset email successfully!");
    }

    @PostMapping("/reset-password")
    public ApiResponse<LoginResponse> resetPassword(@RequestParam("token") String token,
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
