package com.chatsphere.group.controller;

import com.chatsphere.group.entity.Group;
import com.chatsphere.group.entity.GroupMessage;
import com.chatsphere.group.service.GroupService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<Group> createGroup(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody CreateGroupRequest request) {
        UUID ownerId = UUID.fromString(userIdHeader);
        return ResponseEntity.ok(groupService.createGroup(request.getName(), request.getDescription(), ownerId));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<Void> joinGroup(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID groupId) {
        UUID userId = UUID.fromString(userIdHeader);
        groupService.joinGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID groupId,
            @PathVariable UUID memberId) {
        UUID actorId = UUID.fromString(userIdHeader);
        groupService.removeMember(groupId, memberId, actorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/invite-link")
    public ResponseEntity<String> generateInviteLink(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID groupId,
            @RequestBody GenerateInviteRequest request) {
        UUID actorId = UUID.fromString(userIdHeader);
        String inviteLink = groupService.generateInviteLink(
                groupId, 
                actorId, 
                request.getMaxUses(), 
                request.getExpiresAt()
        );
        return ResponseEntity.ok(inviteLink);
    }

    @PostMapping("/join/code")
    public ResponseEntity<Void> joinByCode(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam String code) {
        UUID userId = UUID.fromString(userIdHeader);
        groupService.joinByInviteCode(code, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupMessage>> getGroupMessages(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = UUID.fromString(userIdHeader);
        return ResponseEntity.ok(groupService.getMessages(groupId, userId, page, size));
    }

    @GetMapping("/{groupId}/messages/recipients")
    public ResponseEntity<List<UUID>> getGroupMessageRecipients(
            @PathVariable UUID groupId,
            @RequestParam UUID excludeSender) {
        return ResponseEntity.ok(groupService.getGroupMessageRecipients(groupId, excludeSender));
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupRequest {
        private String name;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateInviteRequest {
        private int maxUses;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime expiresAt;
    }
}
