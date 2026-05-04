package com.weaver.weaver_backend.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RedisHash("redis_token")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RedisToken {

    @Id
    private String jwtId;

    @Indexed
    private UUID userId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiration;
}
