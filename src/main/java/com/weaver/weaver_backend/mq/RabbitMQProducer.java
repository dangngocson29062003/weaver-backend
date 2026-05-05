package com.weaver.weaver_backend.mq;

import com.weaver.weaver_backend.configuration.RabbitConfiguration;
import com.weaver.weaver_backend.dto.request.rabbitmq.EmailRequest;
import com.weaver.weaver_backend.dto.request.rabbitmq.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendVerifiedEmail(EmailRequest request) {
        rabbitTemplate.convertAndSend(
                RabbitConfiguration.APP_EXCHANGE,
                RabbitConfiguration.EMAIL_ROUTING_KEY,
                request
        );
    }

    public void notify(NotificationRequest request) {
        rabbitTemplate.convertAndSend(
                RabbitConfiguration.APP_EXCHANGE,
                RabbitConfiguration.NOTI_ROUTING_KEY,
                request
        );
    }
}
