package com.chatsphere.presence.controller;

import com.chatsphere.presence.service.PresenceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class UserPresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserStatusResponse> getPresence(@PathVariable UUID userId) {
        String status = presenceService.getUserStatus(userId);
        String lastSeen = presenceService.getLastSeen(userId);
        return ResponseEntity.ok(new UserStatusResponse(userId, status, lastSeen));
    }

    @Data
    @AllArgsConstructor
    public static class UserStatusResponse {
        private UUID userId;
        private String status;
        private String lastSeen;
    }
}
