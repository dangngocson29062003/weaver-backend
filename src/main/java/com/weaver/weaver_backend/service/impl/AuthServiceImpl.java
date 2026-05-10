package com.weaver.weaver_backend.service.impl;

import com.weaver.weaver_backend.common.*;
import com.weaver.weaver_backend.dto.request.auth.CreateUserRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginRequest;
import com.weaver.weaver_backend.dto.request.auth.LoginViaOAuthRequest;
import com.weaver.weaver_backend.dto.request.rabbitmq.EmailRequest;
import com.weaver.weaver_backend.dto.request.rabbitmq.NotificationRequest;
import com.weaver.weaver_backend.dto.response.TokenResponse;
import com.weaver.weaver_backend.dto.response.auth.CreateUserResponse;
import com.weaver.weaver_backend.dto.response.auth.LoginResponse;
import com.weaver.weaver_backend.entity.*;
import com.weaver.weaver_backend.exception.BadRequestException;
import com.weaver.weaver_backend.exception.ForbiddenException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.exception.UnauthorizedException;
import com.weaver.weaver_backend.mapper.UserMapper;
import com.weaver.weaver_backend.mq.RabbitMQProducer;
import com.weaver.weaver_backend.repository.UserBackupCodeRepository;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.repository.UserSessionRepository;
import com.weaver.weaver_backend.service.IRedisSessionService;
import com.weaver.weaver_backend.service.other.GoogleAuthenticatorService;
import com.weaver.weaver_backend.service.IAuthService;
import com.weaver.weaver_backend.service.IRedisTokenService;
import com.weaver.weaver_backend.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    private final UserBackupCodeRepository userBackupCodeRepository;

    private final UserSessionRepository userSessionRepository;

    private final IRedisTokenService redisTokenService;

    private final IRedisSessionService redisSessionService;

    private final HttpServletRequest httpServletRequest;

    private final RabbitMQProducer rabbitMQProducer;

    private final UserAgentAnalyzer userAgentAnalyzer;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public LoginResponse login(LoginRequest request, String deviceId) {
        String email = request.email();
        String password = request.password();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }
        if (!user.getEmailVerified()) {
            EmailRequest emailRequest = new EmailRequest(user, EmailType.VERIFICATION_EMAIL);
            rabbitMQProducer.sendEmail(emailRequest);
        }
        if (user.getTwoFaEnabled()) {
            TokenResponse twoFAToken = jwtUtils.generateToken(user, TokenType.TWOFA_TOKEN, null);
            return LoginResponse.builder()
                    .twoFAToken(twoFAToken.value())
                    .build();
        }
        return handleLoginSuccess(user, deviceId);
    }

    @Override
    @Transactional
    public LoginResponse verifyEmail(String token, String deviceId) {
        Claims claims = jwtUtils.extractClaims(token);
        String jwtId = jwtUtils.getJwtId(claims);
        if (jwtUtils.isTokenBlacklisted(jwtId)) {
            throw new ForbiddenException("Cannot access to resource");
        }
        TokenType tokenType = jwtUtils.getType(claims);
        if (tokenType != TokenType.VERIFICATION_TOKEN) {
            throw new BadRequestException("Invalid token type");
        }
        UUID userId = jwtUtils.getUserId(claims);
        String email = jwtUtils.getEmail(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equals(email)) {
            throw new UnauthorizedException("Email mismatch");
        }
        if (!user.getEmailVerified()) {
            user.setEmailVerified(true);
            user.setUserStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
        redisTokenService.saveToken(RedisToken.builder()
                .jwtId(jwtId)
                .userId(user.getId())
                .expiration(remainingTime > 0 ? remainingTime / 1000 : 1)
                .build());
        NotificationRequest notificationRequest = new NotificationRequest(user.getId(),
                "Verified Successfully",
                "Welcome to WEAVER! Your email is verified successfully",
                NotificationType.EMAIL_VERIFIED);
        rabbitMQProducer.notify(notificationRequest);
        log.info("User {} verified email successfully", email);
        return handleLoginSuccess(user, deviceId);
    }

    @Override
    public void sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        EmailRequest emailRequest = new EmailRequest(user, EmailType.PASSWORD_RESET_EMAIL);
        rabbitMQProducer.sendEmail(emailRequest);
    }

    @Override
    public void resetPassword(String token, String password) {
        Claims claims = jwtUtils.extractClaims(token);
        String jwtId = jwtUtils.getJwtId(claims);
        if (jwtUtils.isTokenBlacklisted(jwtId)) {
            throw new ForbiddenException("Cannot access to resource");
        }
        TokenType tokenType = jwtUtils.getType(claims);
        if (tokenType != TokenType.FORGOT_PASSWORD_TOKEN) {
            throw new BadRequestException("Invalid Forgot Password Token");
        }
        UUID userId = jwtUtils.getUserId(claims);
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
        redisTokenService.saveToken(RedisToken.builder()
                .jwtId(jwtId)
                .userId(user.getId())
                .expiration(remainingTime > 0 ? remainingTime : 1)
                .build());
        NotificationRequest notificationRequest = new NotificationRequest(user.getId(),
                "Changed Password Successfully",
                "You recently changed password",
                NotificationType.CHANGE_PASSWORD);
        rabbitMQProducer.notify(notificationRequest);
    }

    @Override
    public LoginResponse verifyTwoFA(String token, String OTP, String deviceId) {
        try {
            Claims claims = jwtUtils.extractClaims(token);
            UUID userId = jwtUtils.getUserId(claims);
            TokenType tokenType = jwtUtils.getType(claims);
            if (tokenType != TokenType.TWOFA_TOKEN) {
                throw new BadRequestException("Invalid 2FA token");
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            boolean isVerified = ggAuthService.verifyCode(user.getTwoFaSecret(), OTP);
            if (!isVerified) {
                throw new BadRequestException("OTP invalid");
            } else {
                return handleLoginSuccess(user, deviceId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LoginResponse verifyBackupCode(String token, String rawBackupCode, String deviceId) {
        try {
            Claims claims = jwtUtils.extractClaims(token);
            UUID userId = jwtUtils.getUserId(claims);
            TokenType tokenType = jwtUtils.getType(claims);
            if (tokenType != TokenType.TWOFA_TOKEN) {
                throw new BadRequestException("Invalid 2FA token");
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            List<UserBackupCode> storedCodes = userBackupCodeRepository.findAllByUser(user);
            UserBackupCode validCode = storedCodes.stream()
                    .filter(code -> passwordEncoder.matches(rawBackupCode, code.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Invalid backup code"));
            userBackupCodeRepository.delete(validCode);
            return handleLoginSuccess(user, deviceId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public CreateUserResponse createUser(CreateUserRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);
        user.setUserStatus(UserStatus.PENDING);
        try {
            user = userRepository.save(user);
            EmailRequest emailRequest = new EmailRequest(user, EmailType.VERIFICATION_EMAIL);
            rabbitMQProducer.sendEmail(emailRequest);
        } catch (DataIntegrityViolationException exception) {
            log.error("User already exists");
            throw new BadRequestException("User already exists");
        }
        return userMapper.toCreateUserResponse(user);
    }

    @Override
    public LoginResponse loginViaOAuth(LoginViaOAuthRequest request, String deviceId) {
        User user = userRepository.findByEmail(request.email()).orElseGet(() -> {
                    User newUser = userMapper.toUserFromOAuth(request);
                    newUser.setEmailVerified(true);
                    newUser.setCredentialStatus(CredentialStatus.NO_PASSWORD);
                    return userRepository.save(newUser);
                }
        );
        return handleLoginSuccess(user, deviceId);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        Claims refreshClaims = jwtUtils.extractClaims(refreshToken);
        TokenType tokenType = jwtUtils.getType(refreshClaims);
        UUID userId = jwtUtils.getUserId(refreshClaims);
        UUID sessionId = jwtUtils.getSessionId(refreshClaims);
        String refreshJwtId = jwtUtils.getJwtId(refreshClaims);
        if (tokenType != TokenType.REFRESH_TOKEN) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (!redisTokenService.existsByJwtId(refreshJwtId)) {
            throw new UnauthorizedException("Refresh token has been revoked or logged out");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        TokenResponse newAccessToken = jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN, sessionId);

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
        Claims refreshClaims = jwtUtils.extractClaims(refreshToken);
        TokenType tokenType = jwtUtils.getType(refreshClaims);
        String refreshJwtId = jwtUtils.getJwtId(refreshClaims);
        if (tokenType != TokenType.REFRESH_TOKEN) {
            throw new BadRequestException("Invalid refresh token");
        }
        log.info("Refresh token {} deleted from Redis", refreshJwtId);
        redisTokenService.deleteTokenByJwtId(refreshJwtId);
        String header = httpServletRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);
            Claims accessClaims = jwtUtils.extractClaims(accessToken);
            Date exp = jwtUtils.getExpiration(accessClaims);
            long ttl = (exp.getTime() - System.currentTimeMillis()) / 1000;
            String accessJwtId = jwtUtils.getJwtId(accessClaims);
            UUID userId = jwtUtils.getUserId(accessClaims);
            if (ttl > 0) {
                redisTokenService.saveToken(
                        RedisToken.builder()
                                .jwtId(accessJwtId)
                                .userId(userId)
                                .expiration(ttl)
                                .build()
                );
                log.info("Access token {} blacklisted for {} ms", accessJwtId, ttl);
            } else {
                log.info("Access token already expired (no need to blacklist)");
            }
        } else {
            log.warn("No Bearer token found in Authorization header, skipping blacklist");
            throw new UnauthorizedException("Please login");
        }
        SecurityContextHolder.clearContext();
    }

    private LoginResponse handleLoginSuccess(User user, String deviceId) {
        UserSession userSession = createSession(user, deviceId);
        TokenResponse accessToken = jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN, userSession.getId());
        TokenResponse refreshToken = jwtUtils.generateToken(user, TokenType.REFRESH_TOKEN, userSession.getId());
        redisTokenService.saveToken(RedisToken.builder()
                .jwtId(refreshToken.jwtId())
                .userId(user.getId())
                .sessionId(userSession.getId())
                .expiration(refreshToken.ttlSeconds())
                .build());
        return LoginResponse.builder()
                .accessToken(accessToken.value())
                .refreshToken(refreshToken.value())
                .build();
    }

    private UserSession createSession(User user, String deviceId) {
        String userAgentString = httpServletRequest.getHeader("User-Agent");
        UserAgent userAgent = userAgentAnalyzer.parse(userAgentString);
        String browser = userAgent.getValue(UserAgent.AGENT_NAME);
        String os = userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        String deviceType = userAgent.getValue(UserAgent.DEVICE_CLASS);
        String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = httpServletRequest.getRemoteAddr();
        }
        Optional<UserSession> existingSession = userSessionRepository
                .findByUserAndDeviceId(user, deviceId);
        UserSession session;
        if (existingSession.isPresent()) {
            session = existingSession.get();
            session.setIpAddress(ipAddress);
            session.setLastActive(Instant.now());
            session.setExpiresAt(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));
            session.setIsRevoked(false);
        } else {
            session = UserSession.builder()
                    .user(user)
                    .browser(browser)
                    .os(os)
                    .deviceType(deviceType)
                    .ipAddress(ipAddress)
                    .lastActive(Instant.now())
                    .expiresAt(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS))
                    .deviceId(deviceId)
                    .isRevoked(false)
                    .build();
        }
        session = userSessionRepository.save(session);
        redisSessionService.saveSession(RedisSession.builder()
                .sessionId(session.getId().toString())
                .userId(user.getId())
                .revoked(false)
                .expiration(refreshTokenExpiration / 1000)
                .build()
        );
        return session;
    }
}
