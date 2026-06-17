package com.chatsphere.audit.controller;

import com.chatsphere.audit.entity.AuditLog;
import com.chatsphere.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/logs/{userId}")
    public ResponseEntity<List<AuditLog>> getUserLogs(@PathVariable UUID userId) {
        return ResponseEntity.ok(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }
}
