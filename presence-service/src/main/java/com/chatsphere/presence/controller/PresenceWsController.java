package com.chatsphere.presence.controller;

import com.chatsphere.presence.service.PresenceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceWsController {

    private final PresenceService presenceService;

    @MessageMapping("/presence.status")
    public void handleStatusChange(StatusChangeRequest request, Principal principal) {
        if (principal == null) return;
        UUID userId = UUID.fromString(principal.getName());
        log.info("WS Presence manual status change from: {} to: {}", userId, request.getStatus());

        if ("AWAY".equalsIgnoreCase(request.getStatus())) {
            presenceService.setAway(userId);
        } else if ("ONLINE".equalsIgnoreCase(request.getStatus())) {
            presenceService.setOnline(userId);
        } else {
            presenceService.setOffline(userId);
        }
    }

    @MessageMapping("/typing")
    public void handleTyping(TypingRequest request, Principal principal) {
        if (principal == null) return;
        UUID userId = UUID.fromString(principal.getName());
        UUID conversationId = UUID.fromString(request.getConversationId());
        
        presenceService.setTyping(conversationId, userId, request.isTyping());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChangeRequest {
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypingRequest {
        private String conversationId;
        private boolean isTyping;
    }
}
