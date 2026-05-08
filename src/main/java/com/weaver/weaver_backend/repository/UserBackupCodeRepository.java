package com.weaver.weaver_backend.repository;

import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.entity.UserBackupCode;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserBackupCodeRepository extends JpaRepository<UserBackupCode, UUID> {

    List<UserBackupCode> findAllByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserBackupCode u WHERE u.user = :user")
    void deleteByUser(User user);
}
