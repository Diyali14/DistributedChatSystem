package com.chatsphere.group.entity;

import com.chatsphere.common.crypto.AesEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessage {

    @Id
    private UUID id;

    @Column(name = "client_message_id", unique = true, nullable = false, length = 100)
    private String clientMessageId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Convert(converter = AesEncryptor.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private String type = "TEXT";

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "reply_to_id")
    private UUID replyToId;

    @Version
    @Builder.Default
    private int version = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
