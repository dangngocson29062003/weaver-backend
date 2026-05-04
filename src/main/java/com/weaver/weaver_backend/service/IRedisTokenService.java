package com.weaver.weaver_backend.service;

import com.weaver.weaver_backend.entity.RedisToken;

public interface IRedisTokenService {

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    boolean existsByJwtId(String jwtId);
}
