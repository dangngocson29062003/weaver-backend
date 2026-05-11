package com.weaver.weaver_backend.repository;

import com.weaver.weaver_backend.entity.Notification;
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
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
//    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId);


}
