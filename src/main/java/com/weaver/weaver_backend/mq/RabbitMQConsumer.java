package com.weaver.weaver_backend.mq;

import com.weaver.weaver_backend.common.EmailType;
import com.weaver.weaver_backend.configuration.RabbitConfiguration;
import com.weaver.weaver_backend.dto.request.rabbitmq.EmailRequest;
import com.weaver.weaver_backend.dto.request.rabbitmq.NotificationRequest;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.entity.Notification;
import com.weaver.weaver_backend.entity.NotificationUser;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.repository.NotificationRepository;
import com.weaver.weaver_backend.repository.NotificationUserRepository;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "RABBIT_CONSUMER")
@RequiredArgsConstructor
public class RabbitMQConsumer {
    private final IEmailService emailService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationUserRepository notificationUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitConfiguration.EMAIL_QUEUE)
    public void handleSendWelcomeEmail(EmailRequest request) {
        log.info("Received email request for: {}", request.user().getEmail());
        try {
            if (request.emailType() == EmailType.VERIFICATION_EMAIL) {
                emailService.sendWelcomeEmail(request.user());
            }else if(request.emailType() == EmailType.PASSWORD_RESET_EMAIL) {
                emailService.sendForgotPasswordEmail(request.user());
            }
            log.info("Email sent via RabbitMQ for: {}", request.user().getEmail());
        } catch (Exception e) {
            log.error("Error processing email queue: ", e);
        }
    }

    @RabbitListener(queues = RabbitConfiguration.NOTI_QUEUE)
    public void handleNotification(NotificationRequest request) {
        User user = userRepository.findById(request.userId()).orElse(null);

        Notification notification = Notification.builder()
                .title(request.title())
                .actionUrl(request.actionUrl())
                .type(request.type())
                .message(request.message())
                .build();

        notification.addRecipients(user);

        messagingTemplate.convertAndSendToUser(
                request.userId().toString(),
                "/user/queue/notifications",
                NotificationResponse.builder()
                        .isRead(false)
                        .message(notification.getMessage())
                        .title(notification.getTitle())
                        .actionUrl(notification.getActionUrl())
                        .build()
        );

        notificationRepository.save(notification);


        long unreadCount = notificationUserRepository.countUnreadByUserId(request.userId());

        messagingTemplate.convertAndSendToUser(
                request.userId().toString(),
                "/user/queue/unread",
                unreadCount
        );

    }
}