package com.chatsphere.audit.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "message.exchange";
    public static final String QUEUE_AUDIT = "audit.queue";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue auditQueue() {
        return new Queue(QUEUE_AUDIT, true);
    }

    @Bean
    public Binding bindingUserActivity(Queue auditQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(auditQueue).to(topicExchange).with("user.activity");
    }

    @Bean
    public Binding bindingGroupActivity(Queue auditQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(auditQueue).to(topicExchange).with("group.activity");
    }
}
