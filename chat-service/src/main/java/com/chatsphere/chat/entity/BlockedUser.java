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
@Table(name = "blocked_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "blocked_user_id", nullable = false)
    private UUID blockedUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
