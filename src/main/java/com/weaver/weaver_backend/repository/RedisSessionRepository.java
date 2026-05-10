package com.weaver.weaver_backend.repository;

import com.weaver.weaver_backend.entity.RedisSession;
import org.springframework.data.repository.CrudRepository;

public interface RedisSessionRepository extends CrudRepository<RedisSession, String> {
}
