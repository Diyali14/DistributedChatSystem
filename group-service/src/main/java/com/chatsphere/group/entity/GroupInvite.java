package com.chatsphere.group.entity;

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
@Table(name = "group_invites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvite {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "invite_code", unique = true, nullable = false, length = 100)
    private String inviteCode;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private int maxUses = 0; // 0 for unlimited

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
