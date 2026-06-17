package com.chatsphere.presence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String KEY_STATUS = "presence:status";
    private static final String KEY_LASTSEEN = "presence:lastseen";
    private static final String KEY_TYPING_PREFIX = "presence:typing:";

    public void setOnline(UUID userId) {
        redisTemplate.opsForHash().put(KEY_STATUS, userId.toString(), "ONLINE");
        broadcastStatusChange(userId, "ONLINE");
        log.info("User {} set to ONLINE in Redis", userId);
    }

    public void setOffline(UUID userId) {
        redisTemplate.opsForHash().put(KEY_STATUS, userId.toString(), "OFFLINE");
        String timestamp = LocalDateTime.now().toString();
        redisTemplate.opsForHash().put(KEY_LASTSEEN, userId.toString(), timestamp);
        broadcastStatusChange(userId, "OFFLINE");
        log.info("User {} set to OFFLINE in Redis, last seen at {}", userId, timestamp);
    }

    public void setAway(UUID userId) {
        redisTemplate.opsForHash().put(KEY_STATUS, userId.toString(), "AWAY");
        broadcastStatusChange(userId, "AWAY");
        log.info("User {} set to AWAY in Redis", userId);
    }

    public void setTyping(UUID conversationId, UUID userId, boolean isTyping) {
        String key = KEY_TYPING_PREFIX + conversationId.toString();
        if (isTyping) {
            redisTemplate.opsForHash().put(key, userId.toString(), "TYPING");
            // Automatically expire typing key in 5 seconds to prevent ghost typing indicators
            redisTemplate.expire(key, 5, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForHash().delete(key, userId.toString());
        }
        broadcastTypingStatus(conversationId, userId, isTyping);
    }

    public String getUserStatus(UUID userId) {
        Object val = redisTemplate.opsForHash().get(KEY_STATUS, userId.toString());
        return val != null ? val.toString() : "OFFLINE";
    }

    public String getLastSeen(UUID userId) {
        Object val = redisTemplate.opsForHash().get(KEY_LASTSEEN, userId.toString());
        return val != null ? val.toString() : null;
    }

    private void broadcastStatusChange(UUID userId, String status) {
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("status", status);
        payload.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/presence", payload);
    }

    private void broadcastTypingStatus(UUID conversationId, UUID userId, boolean isTyping) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId.toString());
        payload.put("userId", userId.toString());
        payload.put("isTyping", isTyping);

        messagingTemplate.convertAndSend("/topic/typing/" + conversationId.toString(), payload);
    }
}
