package com.chatsphere.chat.entity;

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
@Table(name = "scheduled_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledMessage {

    @Id
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "is_group", nullable = false)
    private boolean isGroup;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SENT, FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
