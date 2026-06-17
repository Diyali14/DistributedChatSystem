package com.chatsphere.chat.controller;

import com.chatsphere.chat.entity.Chat;
import com.chatsphere.chat.service.ChatService;
import com.chatsphere.chat.service.RedisPubSubService;
import com.chatsphere.common.event.MessageSentEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final RedisPubSubService redisPubSubService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void handleSendMessage(SendMessageRequest request, Principal principal) {
        if (principal == null) return;
        UUID senderId = UUID.fromString(principal.getName());
        UUID receiverId = UUID.fromString(request.getReceiverId());

        log.info("WS Message received from: {} to: {}", senderId, receiverId);

        try {
            Chat chat = chatService.sendMessage(
                    senderId,
                    "User_" + senderId.toString().substring(0, 5),
                    receiverId,
                    request.getClientMessageId(),
                    request.getMessage(),
                    request.getType(),
                    request.getReplyToId() != null ? UUID.fromString(request.getReplyToId()) : null
            );

            // Construct event payload
            MessageSentEvent event = MessageSentEvent.builder()
                    .messageId(chat.getId())
                    .clientMessageId(chat.getClientMessageId())
                    .senderId(chat.getSenderId())
                    .senderName("User_" + chat.getSenderId().toString().substring(0, 5))
                    .receiverId(chat.getReceiverId())
                    .message(chat.getMessage()) // Clear text message
                    .type(chat.getType())
                    .isGroup(false)
                    .sequenceNumber(chat.getSequenceNumber())
                    .createdAt(chat.getCreatedAt())
                    .build();

            // 1. Send confirmation back to the sender
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages",
                    event
            );

            // 2. Broadcast via Redis PubSub to route to recipient (regardless of which server instance they are on)
            redisPubSubService.publishChatMessage(event);

        } catch (Exception e) {
            log.error("WebSocket message send failure", e);
            // Push error back to sender
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/errors",
                    "Failed to deliver message: " + e.getMessage()
            );
        }
    }

    @MessageMapping("/chat.read")
    public void handleMessageReceipt(ReceiptRequest request, Principal principal) {
        if (principal == null) return;
        UUID userId = UUID.fromString(principal.getName());
        UUID messageId = UUID.fromString(request.getMessageId());

        chatService.updateReceipt(messageId, userId, request.getStatus());
        log.info("Receipt update: Message {} set to {} by user {}", messageId, request.getStatus(), userId);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        private String clientMessageId;
        private String receiverId;
        private String message;
        private String type; // TEXT, IMAGE, FILE
        private String replyToId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptRequest {
        private String messageId;
        private String status; // DELIVERED, READ
    }
}
