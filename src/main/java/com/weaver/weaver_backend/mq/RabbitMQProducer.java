package com.weaver.weaver_backend.mq;

import com.weaver.weaver_backend.configuration.RabbitConfiguration;
import com.weaver.weaver_backend.dto.request.rabbitmq.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendVerifiedEmail(EmailRequest emailRequest) {
        rabbitTemplate.convertAndSend(
                RabbitConfiguration.EMAIL_EXCHANGE,
                RabbitConfiguration.EMAIL_ROUTING_KEY,
                emailRequest
        );
    }
}
