package com.chatsphere.analytics.listener;

import com.chatsphere.common.event.GroupEvent;
import com.chatsphere.common.event.MessageSentEvent;
import com.chatsphere.common.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsRabbitListener {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_MESSAGES = "analytics:messages:count";
    private static final String KEY_LOGINS = "analytics:logins:count";
    private static final String KEY_GROUPS = "analytics:groups:count";
    private static final String KEY_MEDIA = "analytics:media:count";
    private static final String KEY_MPM_PREFIX = "analytics:messages:perminute:";

    @RabbitListener(queues = "analytics.queue")
    public void handleAnalyticsEvent(Object event) {
        log.info("Analytics received event");

        if (event instanceof MessageSentEvent msgEvent) {
            // Increment total messages
            redisTemplate.opsForValue().increment(KEY_MESSAGES);
            log.info("Incremented total messages counter");

            // Increment media uploads if the message type is ATTACHMENT or IMAGE
            if (!"TEXT".equalsIgnoreCase(msgEvent.getType())) {
                redisTemplate.opsForValue().increment(KEY_MEDIA);
            }

            // Track messages per minute
            String minuteStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String mpmKey = KEY_MPM_PREFIX + minuteStr;
            redisTemplate.opsForValue().increment(mpmKey);
            redisTemplate.expire(mpmKey, 5, TimeUnit.MINUTES); // Keep statistics for 5 minutes

        } else if (event instanceof UserActivityEvent actEvent) {
            if ("LOGIN".equals(actEvent.getActivityType())) {
                redisTemplate.opsForValue().increment(KEY_LOGINS);
                log.info("Incremented login activity counter");
            }
        } else if (event instanceof GroupEvent grpEvent) {
            if ("CREATE".equals(grpEvent.getEventType())) {
                redisTemplate.opsForValue().increment(KEY_GROUPS);
                log.info("Incremented total groups counter");
            }
        }
    }
}
