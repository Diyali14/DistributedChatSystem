package com.chatsphere.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSessionDto {
    private UUID id;
    private String deviceName;
    private String ipAddress;
    private String location;
    private LocalDateTime lastActiveAt;
    private boolean isActive;
}
