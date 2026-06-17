package com.chatsphere.chat.controller;

import com.chatsphere.chat.entity.Chat;
import com.chatsphere.chat.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<Chat>> getMessages(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam String recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID senderId = UUID.fromString(userIdHeader);
        UUID receiverId = UUID.fromString(recipientId);
        return ResponseEntity.ok(chatService.getMessages(senderId, receiverId, page, size));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID messageId) {
        UUID userId = UUID.fromString(userIdHeader);
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/draft")
    public ResponseEntity<Void> saveDraft(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody DraftRequest request) {
        UUID userId = UUID.fromString(userIdHeader);
        chatService.saveDraft(userId, UUID.fromString(request.getRecipientId()), request.isGroup(), request.getContent());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/draft")
    public ResponseEntity<String> getDraft(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam String recipientId,
            @RequestParam boolean isGroup) {
        UUID userId = UUID.fromString(userIdHeader);
        return ResponseEntity.ok(chatService.getDraft(userId, UUID.fromString(recipientId), isGroup));
    }

    @PostMapping("/block/{blockedUserId}")
    public ResponseEntity<Void> blockUser(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID blockedUserId) {
        UUID userId = UUID.fromString(userIdHeader);
        chatService.blockUser(userId, blockedUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{blockedUserId}")
    public ResponseEntity<Void> unblockUser(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID blockedUserId) {
        UUID userId = UUID.fromString(userIdHeader);
        chatService.unblockUser(userId, blockedUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<UUID>> getBlockedUsers(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId = UUID.fromString(userIdHeader);
        return ResponseEntity.ok(chatService.getBlockedUserIds(userId));
    }

    @PostMapping("/report")
    public ResponseEntity<Void> reportUser(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody ReportRequest request) {
        UUID reporterId = UUID.fromString(userIdHeader);
        chatService.reportUser(reporterId, UUID.fromString(request.getReportedUserId()), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/archive/{conversationId}")
    public ResponseEntity<Void> archiveConversation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID conversationId,
            @RequestParam boolean isGroup) {
        UUID userId = UUID.fromString(userIdHeader);
        chatService.archiveConversation(userId, conversationId, isGroup);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/archive/{conversationId}")
    public ResponseEntity<Void> unarchiveConversation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID conversationId) {
        UUID userId = UUID.fromString(userIdHeader);
        chatService.unarchiveConversation(userId, conversationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/archived")
    public ResponseEntity<List<UUID>> getArchivedConversations(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId = UUID.fromString(userIdHeader);
        return ResponseEntity.ok(chatService.getArchivedConversationIds(userId));
    }

    @PostMapping("/schedule")
    public ResponseEntity<Void> scheduleMessage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody ScheduleRequest request) {
        UUID senderId = UUID.fromString(userIdHeader);
        chatService.scheduleMessage(
                senderId,
                UUID.fromString(request.getRecipientId()),
                request.isGroup(),
                request.getMessage(),
                request.getScheduledTime()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportConversation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam String recipientId) {
        UUID u1 = UUID.fromString(userIdHeader);
        UUID u2 = UUID.fromString(recipientId);

        byte[] fileBytes = chatService.exportConversationTxt(u1, u2);
        String filename = "chat_history_" + recipientId.substring(0, 5) + ".txt";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DraftRequest {
        private String recipientId;
        private boolean isGroup;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRequest {
        private String reportedUserId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleRequest {
        private String recipientId;
        private boolean isGroup;
        private String message;
        private LocalDateTime scheduledTime;
    }
}
