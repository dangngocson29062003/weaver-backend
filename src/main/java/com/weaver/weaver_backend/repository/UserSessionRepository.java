package com.weaver.weaver_backend.repository;

import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByUserAndDeviceId(User user, String deviceId);

    List<UserSession> findAllByUserId(UUID userId);
}
