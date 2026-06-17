package com.chatsphere.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID messageId;
    private String clientMessageId;
    private UUID senderId;
    private String senderName;
    private UUID receiverId; // Null for group messages
    private String message;  // Decrypted message text for event consumption (e.g. preview)
    private String type;     // TEXT, IMAGE, FILE, AUDIO
    private boolean isGroup;
    private UUID groupId;    // Null for private messages
    private long sequenceNumber;
    private LocalDateTime createdAt;
}
