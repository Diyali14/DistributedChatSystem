package com.chatsphere.group.service;

import com.chatsphere.common.event.MessageSentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPubSubService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL_GROUP = "chatsphere:ws:group";

    public void publishGroupMessage(MessageSentEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL_GROUP, json);
        } catch (Exception e) {
            log.error("Failed to publish group message to Redis", e);
        }
    }

    public void receiveGroupMessage(String messageJson) {
        try {
            MessageSentEvent event = objectMapper.readValue(messageJson, MessageSentEvent.class);
            log.info("Received Redis PubSub group msg for {}. Broad-casting to WS topic.", event.getGroupId());
            
            // Broadcast to all local WebSocket connections subscribed to /topic/group/{groupId}
            messagingTemplate.convertAndSend(
                    "/topic/group/" + event.getGroupId().toString(),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to route group message to local WebSockets", e);
        }
    }

    @Bean
    public RedisMessageListenerContainer redisGroupContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter groupListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(groupListenerAdapter, new PatternTopic(CHANNEL_GROUP));
        return container;
    }

    @Bean
    public MessageListenerAdapter groupListenerAdapter(RedisPubSubService redisPubSubService) {
        return new MessageListenerAdapter(redisPubSubService, "receiveGroupMessage");
    }
}
