package com.weaver.weaver_backend.service.impl;

import com.weaver.weaver_backend.dto.request.user.NotificationMarkRequest;
import com.weaver.weaver_backend.dto.request.user.UnreadCountResponse;
import com.weaver.weaver_backend.dto.response.PageResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.entity.NotificationUser;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.mapper.NotificationMapper;
import com.weaver.weaver_backend.repository.NotificationUserRepository;
import com.weaver.weaver_backend.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "LOGGING_NOTIFICATION_SERVICE")
public class NotificationServiceImpl implements INotificationService {
    private final NotificationUserRepository notificationUserRepository;

    @Override
    public PageResponse<NotificationResponse> findAllByUserId(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<NotificationUser> notificationPage = notificationUserRepository.findAllByUserId(userId, pageable);

        List<NotificationResponse> content = notificationPage.stream()
                .map(NotificationMapper::toNotificationResponse)
                .toList();

        PageResponse<NotificationResponse> response = PageResponse.<NotificationResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalElement(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .content(content)
                .build();
        return response;
    }

    @Override
    public NotificationResponse markSingleAsRead(UUID userId, long notificationId) {
        NotificationUser notificationUser = notificationUserRepository.findByIdAndUserId(userId, notificationId)
                .orElseThrow(() -> new NotFoundException("Notification Or User Not Found"));

        return NotificationMapper.toNotificationResponse(notificationUser);
    }

    @Override
    public List<NotificationResponse> markAllAsRead(UUID userId, NotificationMarkRequest request) {
        List<Long> notificationIds = request.notificationUserIds();
        List<NotificationUser> notificationUsers = notificationUserRepository.findByIdInAndUserId(notificationIds, userId);

        List<NotificationUser> unreadList = notificationUsers.stream()
                .filter(item -> !item.isRead())
                .peek(item -> {
                    item.setRead(true);
                    item.setReadAt(LocalDateTime.now());
                })
                .toList();

        if(unreadList == null || unreadList.isEmpty()) {
            return List.of();
        }

        notificationUserRepository.saveAll(unreadList);

        List<NotificationResponse> responses = unreadList.stream()
                .map(NotificationMapper::toNotificationResponse)
                .toList();
        return responses;
    }

    @Override
    public UnreadCountResponse getUnreadNotificationQuantity(UUID userId) {
        long unreadCount = notificationUserRepository.countUnreadByUserId(userId);
        return new UnreadCountResponse(unreadCount);
    }
}
