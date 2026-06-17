package com.chatsphere.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false)
    private String eventType; // LOGIN_FAIL, LOCKOUT, ABUSE, RATE_LIMIT

    @Column(name = "ip_address")
    private String ipAddress;

    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
