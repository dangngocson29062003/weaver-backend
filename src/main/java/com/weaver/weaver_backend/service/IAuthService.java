package com.weaver.weaver_backend.service;

import com.nimbusds.jose.JOSEException;
import com.weaver.weaver_backend.dto.request.auth.CreateUserRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginViaOAuthRequest;
import com.weaver.weaver_backend.dto.response.auth.CreateUserResponse;
import com.weaver.weaver_backend.dto.response.auth.LoginResponse;

import java.text.ParseException;

public interface IAuthService {
    CreateUserResponse createUser(CreateUserRequest request);
    LoginResponse loginViaOAuth(LoginViaOAuthRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse verifyEmail(String token);
    LoginResponse verifyTwoFA(String token, int OTP);
    LoginResponse refreshToken(String refreshToken);
    void logout(String refreshToken) throws ParseException, JOSEException;;
}
