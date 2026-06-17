package com.chatsphere.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_MESSAGES = "analytics:messages:count";
    private static final String KEY_LOGINS = "analytics:logins:count";
    private static final String KEY_GROUPS = "analytics:groups:count";
    private static final String KEY_MEDIA = "analytics:media:count";
    private static final String KEY_MPM_PREFIX = "analytics:messages:perminute:";

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalMessages", getLongVal(KEY_MESSAGES));
        stats.put("totalLogins", getLongVal(KEY_LOGINS));
        stats.put("totalGroups", getLongVal(KEY_GROUPS));
        stats.put("totalMediaUploads", getLongVal(KEY_MEDIA));

        // Compute current messages per minute rate
        String currentMinute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        stats.put("messagesPerMinute", getLongVal(KEY_MPM_PREFIX + currentMinute));

        return ResponseEntity.ok(stats);
    }

    private long getLongVal(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0L;
    }
}
