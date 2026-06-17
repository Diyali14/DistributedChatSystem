package com.chatsphere.auth;

import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.auth.entity.User;
import com.chatsphere.auth.repository.DeviceSessionRepository;
import com.chatsphere.auth.repository.LoginHistoryRepository;
import com.chatsphere.auth.repository.SecurityEventRepository;
import com.chatsphere.auth.repository.UserRepository;
import com.chatsphere.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DeviceSessionRepository deviceSessionRepository;
    @Mock
    private LoginHistoryRepository loginHistoryRepository;
    @Mock
    private SecurityEventRepository securityEventRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testUserRegistrationSuccess() {
        RegisterRequest req = new RegisterRequest("alice", "alice@mail.com", "pass123");
        
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("hashedPass");

        String result = authService.register(req);

        assertNotNull(result);
        assertTrue(result.contains("successful"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertEquals("alice", savedUser.getUsername());
        assertEquals("alice@mail.com", savedUser.getEmail());
        assertEquals("hashedPass", savedUser.getPassword());
        assertFalse(savedUser.isEnabled()); // Email verification required!
    }

    @Test
    public void testRegistrationFailsOnDuplicateUsername() {
        RegisterRequest req = new RegisterRequest("alice", "alice@mail.com", "pass123");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }
}
