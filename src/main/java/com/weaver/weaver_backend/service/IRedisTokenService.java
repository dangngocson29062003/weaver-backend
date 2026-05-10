package com.weaver.weaver_backend.service;

import com.weaver.weaver_backend.entity.RedisToken;

import java.util.UUID;

public interface IRedisTokenService {

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    void deleteAllBySessionId(UUID sessionId);

    boolean existsByJwtId(String jwtId);


}
