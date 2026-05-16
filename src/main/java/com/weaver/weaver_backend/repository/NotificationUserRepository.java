package com.weaver.weaver_backend.repository;

import com.weaver.weaver_backend.entity.NotificationUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationUserRepository extends JpaRepository<NotificationUser,Long > {

    @Query("""
                SELECT nu
                FROM NotificationUser nu
                JOIN FETCH nu.notification n
                WHERE nu.user.id = :userId
                ORDER BY  n.createdAt DESC
            """)
    Page<NotificationUser> findAllByUserId(@Param("userId")UUID userId, Pageable pageable);

    @Query("""
                SELECT nu
                FROM NotificationUser nu
                JOIN FETCH nu.notification n
                WHERE nu.user.id = :userId
                AND nu.id = :notificationId
            """)
    Optional<NotificationUser> findByIdAndUserId(
            UUID userId,
            Long notificationId
    );

    @Query("""
                SELECT nu
                FROM NotificationUser nu
                JOIN FETCH nu.notification
                WHERE nu.id IN :notificationUserIdList
                AND nu.user.id = :userId
            """)
    List<NotificationUser> findByIdInAndUserId(@Param("notificationUserIdList") List<Long> notificationIds, @Param("userId") UUID user);

    @Query("""
                SELECT COUNT(nu)
                FROM NotificationUser nu
                WHERE nu.user.id = :userId
                AND nu.isRead = false
            """)
    long countUnreadByUserId(@Param("userId") UUID userId);
}
