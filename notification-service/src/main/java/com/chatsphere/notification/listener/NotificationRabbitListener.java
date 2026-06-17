package com.chatsphere.notification.listener;

import com.chatsphere.common.event.GroupEvent;
import com.chatsphere.common.event.MessageSentEvent;
import com.chatsphere.notification.entity.Notification;
import com.chatsphere.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRabbitListener {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;

    @RabbitListener(queues = "notification.queue")
    public void handleMessageEvent(Object eventPayload) {
        log.info("Notification queue received event payload");

        try {
            if (eventPayload instanceof MessageSentEvent event) {
                if (event.isGroup()) {
                    // Group Message Notification
                    // Retrieve members from group-service via load-balanced HTTP request
                    String url = "http://group-service/api/groups/" + event.getGroupId() + "/messages/recipients?excludeSender=" + event.getSenderId();
                    log.info("Fetching group members from group-service: {}", url);
                    
                    // Call group-service to get list of member UUIDs to notify
                    @SuppressWarnings("unchecked")
                    List<String> memberIds = restTemplate.getForObject(url, List.class);

                    if (memberIds != null) {
                        for (String idStr : memberIds) {
                            UUID memberId = UUID.fromString(idStr);
                            saveNotification(
                                    memberId,
                                    "New Message in Group",
                                    event.getSenderName() + ": " + event.getMessage()
                            );
                        }
                    }
                } else {
                    // Private Message Notification
                    saveNotification(
                            event.getReceiverId(),
                            "New Message from " + event.getSenderName(),
                            event.getMessage()
                    );
                }
            } else if (eventPayload instanceof GroupEvent event) {
                // Handle group invites or membership logs
                if ("JOIN".equals(event.getEventType())) {
                    saveNotification(
                            event.getUserId(),
                            "Group Joined",
                            "You have successfully joined group: " + event.getGroupName()
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to process notification, throwing exception to trigger RabbitMQ retry/DLQ", e);
            throw new RuntimeException("Notification processing failed", e);
        }
    }

    private void saveNotification(UUID userId, String title, String content) {
        Notification notif = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title(title)
                .content(content)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notif);
        log.info("Saved notification for user: {} - {}", userId, title);
    }
}
