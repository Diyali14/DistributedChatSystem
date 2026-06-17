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
@Table(name = "message_receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReceipt {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String status; // SENT, DELIVERED, READ

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
