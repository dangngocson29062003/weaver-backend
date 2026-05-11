package com.weaver.weaver_backend.service;

import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.entity.UserSession;

import java.util.Optional;
import java.util.UUID;

public interface IUserSessionService {
    Optional<UserSession> getSessionByUserAndSessionId(User userId, String deviceId);

    UserSession saveSession(User user, String deviceId, Optional<UserSession> existingSession);
}
