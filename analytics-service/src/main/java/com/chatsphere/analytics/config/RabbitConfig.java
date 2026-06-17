package com.chatsphere.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "message.exchange";
    public static final String QUEUE_ANALYTICS = "analytics.queue";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue analyticsQueue() {
        return new Queue(QUEUE_ANALYTICS, true);
    }

    @Bean
    public Binding bindingChat(Queue analyticsQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(analyticsQueue).to(topicExchange).with("message.sent");
    }

    @Bean
    public Binding bindingGroupMsg(Queue analyticsQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(analyticsQueue).to(topicExchange).with("group.message.sent");
    }

    @Bean
    public Binding bindingGroupAct(Queue analyticsQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(analyticsQueue).to(topicExchange).with("group.activity");
    }

    @Bean
    public Binding bindingUserAct(Queue analyticsQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(analyticsQueue).to(topicExchange).with("user.activity");
    }
}
