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
public class UserActivityEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String username;
    private String activityType; // LOGIN, LOGOUT, PROFILE_UPDATE, STATUS_CHANGE
    private String status;       // ONLINE, OFFLINE, AWAY
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
}
