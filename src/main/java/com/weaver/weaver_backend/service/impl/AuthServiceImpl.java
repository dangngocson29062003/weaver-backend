package com.weaver.weaver_backend.service.impl;

import com.weaver.weaver_backend.common.AuthProvider;
import com.weaver.weaver_backend.common.TokenType;
import com.weaver.weaver_backend.common.UserStatus;
import com.weaver.weaver_backend.dto.request.auth.CreateUserRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginViaOAuthRequest;
import com.weaver.weaver_backend.dto.response.TokenResponse;
import com.weaver.weaver_backend.dto.response.auth.CreateUserResponse;
import com.weaver.weaver_backend.dto.response.auth.LoginResponse;
import com.weaver.weaver_backend.entity.RedisToken;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.exception.BadRequestException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.mapper.UserMapper;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.other.GoogleAuthenticatorService;
import com.weaver.weaver_backend.service.IAuthService;
import com.weaver.weaver_backend.service.IRedisTokenService;
import com.weaver.weaver_backend.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthServiceImpl implements IAuthService {
    private final GoogleAuthenticatorService ggAuthService;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final IRedisTokenService redisTokenService;
    private final HttpServletRequest httpServletRequest;

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email();
        String password = request.password();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }
        if (user.getTwoFaEnabled()) {
            TokenResponse twoFAToken = jwtUtils.generateToken(user, TokenType.TWOFA_TOKEN);
            return LoginResponse.builder()
                    .twoFAToken(twoFAToken.value())
                    .build();
        }
        TokenResponse accessToken = jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN);
        TokenResponse refreshToken = jwtUtils.generateToken(user, TokenType.REFRESH_TOKEN);
        RedisToken redisToken = RedisToken.builder()
                .jwtId(refreshToken.jwtId())
                .userId(user.getId())
                .expiration(refreshToken.ttlSeconds())
                .build();

        redisTokenService.saveToken(redisToken);
        return LoginResponse.builder()
                .accessToken(accessToken.value())
                .refreshToken(refreshToken.value())
                .build();
    }

    @Override
    public LoginResponse verifyTwoFA(String token, int OTP) {
        try {
            UUID userId = jwtUtils.getUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            boolean isVerified = ggAuthService.verifyCode(user.getTwoFaSecret(), OTP);
            if (!isVerified) {
                throw new BadRequestException("OTP invalid");
            } else {
                TokenResponse accessToken = jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN);
                TokenResponse refreshToken = jwtUtils.generateToken(user, TokenType.REFRESH_TOKEN);
                RedisToken redisToken = RedisToken.builder()
                        .jwtId(refreshToken.jwtId())
                        .userId(user.getId())
                        .expiration(refreshToken.ttlSeconds())
                        .build();
                redisTokenService.saveToken(redisToken);
                return LoginResponse.builder()
                        .accessToken(accessToken.value())
                        .refreshToken(refreshToken.value())
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);
        user.setUserStatus(UserStatus.PENDING);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            log.error("User already exists");
            throw new BadRequestException("User already exists");
        }
        return userMapper.toCreateUserResponse(user);
    }

    @Override
    public LoginResponse loginViaOAuth(LoginViaOAuthRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseGet(() -> {
                    User newUser = userMapper.toUserFromOAuth(request);
                    return userRepository.save(newUser);
                }
        );
        TokenResponse accessToken = jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN);
        TokenResponse refreshToken = jwtUtils.generateToken(user, TokenType.REFRESH_TOKEN);
        RedisToken redisToken = RedisToken.builder()
                .jwtId(refreshToken.jwtId())
                .userId(user.getId())
                .expiration(refreshToken.ttlSeconds())
                .build();
        redisTokenService.saveToken(redisToken);
        return LoginResponse.builder()
                .accessToken(accessToken.value())
                .refreshToken(refreshToken.value())
                .build();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        UUID userId = jwtUtils.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        TokenResponse newAccessToken = jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN);

        return LoginResponse.builder()
                .accessToken(newAccessToken.value())
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadRequestException("Missing logout information");
        }
        String refreshJwtId = jwtUtils.getJwtId(refreshToken);
        redisTokenService.deleteTokenByJwtId(refreshJwtId);
        String header = httpServletRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);
            Date exp = jwtUtils.getExpiration(accessToken);
            long ttl = exp.getTime() - System.currentTimeMillis();
            String accessJwtId = jwtUtils.getJwtId(accessToken);
            UUID userId = jwtUtils.getUserId(accessToken);
            if (ttl > 0) {
                redisTokenService.saveToken(
                        RedisToken.builder()
                                .jwtId(accessJwtId)
                                .userId(userId)
                                .expiration(ttl)
                                .build()
                );
                log.info("Access token blacklisted, TTL: {} ms", ttl);
            } else {
                log.info("Access token already expired (no need to blacklist)");
            }
        } else {
            log.warn("No Bearer token found in Authorization header, skipping blacklist");
        }
        SecurityContextHolder.clearContext();
    }
}
