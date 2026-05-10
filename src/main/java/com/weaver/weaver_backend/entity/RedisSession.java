package com.weaver.weaver_backend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RedisHash("redis_session")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RedisSession {
    @Id
    private String sessionId;

    @Indexed
    private UUID userId;

    private Boolean revoked;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiration;
}
