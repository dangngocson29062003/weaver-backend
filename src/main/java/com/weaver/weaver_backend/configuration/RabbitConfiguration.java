package com.weaver.weaver_backend.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    // Các hằng số cho Email
    public static final String APP_EXCHANGE = "app.exchange";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_ROUTING_KEY = "email.routing.key";

    public static final String NOTI_QUEUE = "notification.queue";
    public static final String NOTI_ROUTING_KEY = "notification.routing.key";

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Queue notiQueue() {
        return new Queue(NOTI_QUEUE, true);
    }

    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(APP_EXCHANGE);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(emailQueue).to(appExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding notiBinding(Queue notiQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(notiQueue).to(appExchange).with(NOTI_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
