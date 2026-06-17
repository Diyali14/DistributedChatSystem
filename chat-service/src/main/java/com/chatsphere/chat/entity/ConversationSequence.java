package com.chatsphere.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "conversation_sequence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSequence {

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "last_sequence_number", nullable = false)
    @Builder.Default
    private long lastSequenceNumber = 0;
}
