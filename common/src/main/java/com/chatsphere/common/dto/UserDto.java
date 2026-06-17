package com.chatsphere.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String email;
    private String bio;
    private String avatarUrl;
    private String status;
    private boolean enabled;
}
