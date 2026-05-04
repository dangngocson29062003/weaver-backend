package com.weaver.weaver_backend.mq;

import com.weaver.weaver_backend.configuration.RabbitConfiguration;
import com.weaver.weaver_backend.dto.request.email.EmailRequest;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailConsumer {
    private final IEmailService emailService;
    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitConfiguration.EMAIL_QUEUE)
    public void handleSendWelcomeEmail(EmailRequest request) {
        log.info("Received email request for: {}", request.email());

        try {
            // Tìm lại user từ DB để đảm bảo data mới nhất
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Gọi hàm gửi mail (Hàm này sẽ generate JWT token như bạn đã viết)
            emailService.sendWelcomeEmail(user);

            log.info("Email sent via RabbitMQ for: {}", request.email());
        } catch (Exception e) {
            log.error("Error processing email queue: ", e);
            // Có thể cấu hình Retry mechanism ở đây nếu cần
        }
    }
}