package com.weaver.weaver_backend.service.impl;


import com.weaver.weaver_backend.entity.RedisToken;
import com.weaver.weaver_backend.repository.RedisTokenRepository;
import com.weaver.weaver_backend.service.IRedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements IRedisTokenService {

    private final RedisTokenRepository redisTokenRepository;

    @Override
    public void saveToken(RedisToken token) {
        redisTokenRepository.save(token);
    }

    @Override
    public void deleteTokenByJwtId(String jwtId) {
        redisTokenRepository.findById(jwtId)
                .ifPresent(redisTokenRepository::delete);
    }

    @Override
    public void deleteAllBySessionId(UUID sessionId) {
        List<RedisToken> tokens =
                redisTokenRepository.findAllBySessionId(sessionId);

        redisTokenRepository.deleteAll(tokens);
    }

    @Override
    public boolean existsByJwtId(String jwtId) {
        return redisTokenRepository.existsById(jwtId);
    }
}
