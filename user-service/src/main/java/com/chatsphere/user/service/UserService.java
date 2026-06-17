package com.chatsphere.user.service;

import com.chatsphere.common.dto.UserDto;
import com.chatsphere.common.event.UserActivityEvent;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UserDto updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBio(updates.getBio());
        user.setAvatarUrl(updates.getAvatarUrl());
        // JPA will automatically check @Version column on save
        userRepository.save(user);

        // Publish profile update event to RabbitMQ
        UserActivityEvent event = UserActivityEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .activityType("PROFILE_UPDATE")
                .status(user.getStatus())
                .details("Profile bio or avatar updated.")
                .timestamp(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend("message.exchange", "user.activity", event);

        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String query) {
        return userRepository.searchUsers(query).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .enabled(user.isEnabled())
                .build();
    }
}
