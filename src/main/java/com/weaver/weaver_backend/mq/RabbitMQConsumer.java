package com.weaver.weaver_backend.mq;

import com.weaver.weaver_backend.configuration.RabbitConfiguration;
import com.weaver.weaver_backend.dto.request.rabbitmq.EmailRequest;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConsumer {
    private final IEmailService emailService;
    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitConfiguration.EMAIL_QUEUE)
    public void handleSendWelcomeEmail(EmailRequest request) {
        log.info("Received email request for: {}", request.email());
        try {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            emailService.sendWelcomeEmail(user);
            log.info("Email sent via RabbitMQ for: {}", request.email());
        } catch (Exception e) {
            log.error("Error processing email queue: ", e);
        }
    }
}