package com.weaver.weaver_backend.service;

import com.weaver.weaver_backend.entity.RedisSession;
import com.weaver.weaver_backend.entity.RedisToken;

public interface IRedisSessionService {

    void saveSession(RedisSession session);
    void revokeSession(String sessionId);
    boolean isSessionRevoked(String sessionId);
}
