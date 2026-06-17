package com.chatsphere.group.controller;

import com.chatsphere.common.event.MessageSentEvent;
import com.chatsphere.group.entity.GroupMessage;
import com.chatsphere.group.service.GroupService;
import com.chatsphere.group.service.RedisPubSubService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GroupWsController {

    private final GroupService groupService;
    private final RedisPubSubService redisPubSubService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/group.send")
    public void handleSendGroupMessage(SendGroupMessageRequest request, Principal principal) {
        if (principal == null) return;
        UUID senderId = UUID.fromString(principal.getName());
        UUID groupId = UUID.fromString(request.getGroupId());

        log.info("WS Group Message received from: {} in group: {}", senderId, groupId);

        try {
            GroupMessage msg = groupService.sendGroupMessage(
                    senderId,
                    "User_" + senderId.toString().substring(0, 5),
                    groupId,
                    request.getClientMessageId(),
                    request.getMessage(),
                    request.getType(),
                    request.getReplyToId() != null ? UUID.fromString(request.getReplyToId()) : null
            );

            // Construct event payload
            MessageSentEvent event = MessageSentEvent.builder()
                    .messageId(msg.getId())
                    .clientMessageId(msg.getClientMessageId())
                    .senderId(msg.getSenderId())
                    .senderName("User_" + msg.getSenderId().toString().substring(0, 5))
                    .groupId(msg.getGroupId())
                    .message(msg.getMessage())
                    .type(msg.getType())
                    .isGroup(true)
                    .sequenceNumber(msg.getSequenceNumber())
                    .createdAt(msg.getCreatedAt())
                    .build();

            // Broadcast via Redis Pub/Sub so that all nodes deliver to their local topic listeners
            redisPubSubService.publishGroupMessage(event);

        } catch (Exception e) {
            log.error("WebSocket group message send failure", e);
            // Push error back to sender
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/errors",
                    "Failed to deliver group message: " + e.getMessage()
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendGroupMessageRequest {
        private String clientMessageId;
        private String groupId;
        private String message;
        private String type; // TEXT, IMAGE, FILE
        private String replyToId;
    }
}
