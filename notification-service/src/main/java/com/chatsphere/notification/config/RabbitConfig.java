package com.chatsphere.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "message.exchange";
    public static final String DLX_EXCHANGE = "notification.dlx";
    
    public static final String QUEUE_NOTIF = "notification.queue";
    public static final String QUEUE_DLQ = "notification.dlq";

    public static final String ROUTING_CHAT_SENT = "message.sent";
    public static final String ROUTING_GROUP_SENT = "group.message.sent";
    public static final String ROUTING_GROUP_ACTIVITY = "group.activity";
    public static final String ROUTING_FAILED = "notification.failed";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NOTIF)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_FAILED)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(QUEUE_DLQ, true);
    }

    @Bean
    public Binding bindingChat(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with(ROUTING_CHAT_SENT);
    }

    @Bean
    public Binding bindingGroupMsg(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with(ROUTING_GROUP_SENT);
    }

    @Bean
    public Binding bindingGroupAct(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with(ROUTING_GROUP_ACTIVITY);
    }

    @Bean
    public Binding bindingDlq(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(ROUTING_FAILED);
    }
}
