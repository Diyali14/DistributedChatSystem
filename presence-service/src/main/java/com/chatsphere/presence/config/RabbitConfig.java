package com.chatsphere.presence.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "message.exchange";
    public static final String QUEUE_PRESENCE = "presence.activity.queue";
    public static final String ROUTING_KEY = "user.activity";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue presenceQueue() {
        return new Queue(QUEUE_PRESENCE, true);
    }

    @Bean
    public Binding presenceBinding(Queue presenceQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(presenceQueue).to(topicExchange).with(ROUTING_KEY);
    }
}
