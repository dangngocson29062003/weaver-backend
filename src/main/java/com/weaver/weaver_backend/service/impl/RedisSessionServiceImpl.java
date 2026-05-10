package com.weaver.weaver_backend.service.impl;

import com.weaver.weaver_backend.entity.RedisSession;
import com.weaver.weaver_backend.entity.RedisToken;
import com.weaver.weaver_backend.repository.RedisSessionRepository;
import com.weaver.weaver_backend.service.IRedisSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisSessionServiceImpl implements IRedisSessionService {

    private final RedisSessionRepository redisSessionRepository;

    @Override
    public void saveSession(RedisSession session) {
        redisSessionRepository.save(session);
    }

    @Override
    public void revokeSession(String sessionId) {
        redisSessionRepository.findById(sessionId)
                .ifPresent(redisSessionRepository::delete);
    }

    @Override
    public boolean isSessionRevoked(String sessionId) {
        RedisSession session =
                redisSessionRepository
                        .findById(sessionId)
                        .orElse(null);

        if (session == null) {
            return true;
        }

        return session.getRevoked();
    }
}
