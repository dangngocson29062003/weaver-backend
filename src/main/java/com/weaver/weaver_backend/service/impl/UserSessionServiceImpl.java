package com.weaver.weaver_backend.service.impl;

import com.weaver.weaver_backend.entity.RedisSession;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.entity.UserSession;
import com.weaver.weaver_backend.repository.UserSessionRepository;
import com.weaver.weaver_backend.service.IRedisSessionService;
import com.weaver.weaver_backend.service.IUserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements IUserSessionService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final UserSessionRepository userSessionRepository;

    private final IRedisSessionService redisSessionService;

    private final HttpServletRequest httpServletRequest;

    private final UserAgentAnalyzer userAgentAnalyzer;


    @Override
    public Optional<UserSession> getSessionByUserAndSessionId(User user, String deviceId) {
        return userSessionRepository.findByUserAndDeviceId(user, deviceId);
    }

    @Override
    public UserSession saveSession(User user, String deviceId, Optional<UserSession> existingSession) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenExpiration, ChronoUnit.MILLIS);

        UserAgent userAgent = parseUserAgent();
        String ipAddress = resolveIpAddress();

        UserSession session = existingSession.orElseGet(() ->
                UserSession.builder()
                        .user(user)
                        .deviceId(deviceId)
                        .browser(userAgent.getValue(UserAgent.AGENT_NAME))
                        .os(userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME))
                        .deviceType(userAgent.getValue(UserAgent.DEVICE_CLASS))
                        .isTrusted(false)
                        .build()
        );

        session.setIpAddress(ipAddress);
        session.setLastActive(now);
        session.setExpiresAt(expiresAt);
        session.setIsRevoked(false);

        session = userSessionRepository.save(session);

        redisSessionService.saveSession(RedisSession.builder()
                .sessionId(session.getId().toString())
                .userId(user.getId())
                .revoked(false)
                .expiration(refreshTokenExpiration / 1000)
                .build());

        return session;
    }

    private UserAgent parseUserAgent() {
        String userAgentString = httpServletRequest.getHeader("User-Agent");
        return userAgentAnalyzer.parse(userAgentString);
    }

    private String resolveIpAddress() {
        String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isBlank()) {
            return httpServletRequest.getRemoteAddr();
        }

        return ipAddress.split(",")[0].trim();
    }
}
