package com.chatsphere.presence.listener;

import com.chatsphere.common.event.UserActivityEvent;
import com.chatsphere.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceRabbitListener {

    private final PresenceService presenceService;

    @RabbitListener(queues = "presence.activity.queue")
    public void handleUserActivity(UserActivityEvent event) {
        log.info("Received user activity event for user {}: {}", event.getUsername(), event.getActivityType());

        if ("LOGIN".equals(event.getActivityType())) {
            presenceService.setOnline(event.getUserId());
        } else if ("LOGOUT".equals(event.getActivityType())) {
            presenceService.setOffline(event.getUserId());
        } else if ("STATUS_CHANGE".equals(event.getActivityType())) {
            if ("AWAY".equalsIgnoreCase(event.getStatus())) {
                presenceService.setAway(event.getUserId());
            } else if ("ONLINE".equalsIgnoreCase(event.getStatus())) {
                presenceService.setOnline(event.getUserId());
            } else {
                presenceService.setOffline(event.getUserId());
            }
        }
    }
}
