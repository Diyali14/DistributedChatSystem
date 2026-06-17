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
public class GroupEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID groupId;
    private String groupName;
    private String eventType; // CREATE, DELETE, JOIN, LEAVE, ROLE_CHANGE
    private UUID actorId;
    private UUID userId;
    private String role; // OWNER, ADMIN, MEMBER
    private LocalDateTime timestamp;
}
