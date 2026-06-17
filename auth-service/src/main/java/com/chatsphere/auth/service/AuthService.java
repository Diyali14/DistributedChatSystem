package com.chatsphere.auth.service;

import com.chatsphere.auth.dto.*;
import com.chatsphere.auth.entity.*;
import com.chatsphere.auth.repository.*;
import com.chatsphere.common.dto.UserDto;
import com.chatsphere.common.event.UserActivityEvent;
import com.chatsphere.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityEventRepository securityEventRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final String VERIFY_TOKEN_PREFIX = "verify_token:";
    private static final String RESET_TOKEN_PREFIX = "reset_token:";

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false) // Mandatory verification
                .status("OFFLINE")
                .build();

        userRepository.save(user);

        // Generate email verification token
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                VERIFY_TOKEN_PREFIX + user.getEmail(),
                token,
                LOCK_DURATION_MINUTES,
                TimeUnit.MINUTES
        );

        String verifyLink = "http://localhost:8080/api/auth/verify?email=" + user.getEmail() + "&token=" + token;
        log.info(">>>> EMAIL VERIFICATION LINK FOR {}: {}", user.getUsername(), verifyLink);

        // Publish registration event to RabbitMQ
        UserActivityEvent event = UserActivityEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .activityType("REGISTRATION")
                .status("OFFLINE")
                .details("Verification link: " + verifyLink)
                .timestamp(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend("message.exchange", "user.activity", event);

        return "Registration successful. Please verify your email via the link printed in server logs.";
    }

    @Transactional
    public String verifyEmail(String email, String token) {
        String storedToken = redisTemplate.opsForValue().get(VERIFY_TOKEN_PREFIX + email);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new RuntimeException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        redisTemplate.delete(VERIFY_TOKEN_PREFIX + email);
        return "Email verified successfully! You can now log in.";
    }

    @Transactional
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + email,
                token,
                LOCK_DURATION_MINUTES,
                TimeUnit.MINUTES
        );

        String resetLink = "http://localhost:3000/reset-password?email=" + email + "&token=" + token;
        log.info(">>>> PASSWORD RESET LINK: {}", resetLink);

        return "Password reset link generated. Please check server logs.";
    }

    @Transactional
    public String resetPassword(String email, String token, String newPassword) {
        String storedToken = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + email);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete(RESET_TOKEN_PREFIX + email);
        return "Password reset successfully.";
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .orElseGet(() -> userRepository.findByEmail(request.getUsernameOrEmail())
                        .orElse(null));

        if (user == null) {
            logSecurityEvent(null, "LOGIN_FAIL", ipAddress, "Attempted username/email: " + request.getUsernameOrEmail());
            throw new RuntimeException("Invalid credentials");
        }

        // Lockout Check
        if (user.getLockTime() != null) {
            if (user.getLockTime().isAfter(LocalDateTime.now().minusMinutes(LOCK_DURATION_MINUTES))) {
                logSecurityEvent(user.getId(), "LOCKOUT", ipAddress, "Attempted login on locked account");
                throw new RuntimeException("Account is temporarily locked. Try again later.");
            } else {
                // Lock expired
                user.setLockTime(null);
                user.setFailedLoginAttempts(0);
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockTime(LocalDateTime.now());
                logSecurityEvent(user.getId(), "LOCKOUT", ipAddress, "Account locked due to 5 failures");
            } else {
                logSecurityEvent(user.getId(), "LOGIN_FAIL", ipAddress, "Failed attempt: " + attempts);
            }
            userRepository.save(user);
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Please verify your email before logging in.");
        }

        // Login Succeeded - Reset lockout states
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        user.setStatus("ONLINE");
        userRepository.save(user);

        UUID sessionId = UUID.randomUUID();
        String accessToken = JwtUtil.generateAccessToken(user.getId().toString(), user.getUsername(), List.of("ROLE_USER"), sessionId.toString());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId().toString(), sessionId.toString());

        // Save active device session
        DeviceSession session = DeviceSession.builder()
                .id(sessionId)
                .userId(user.getId())
                .token(passwordEncoder.encode(refreshToken)) // Hash refresh token for DB storage
                .deviceName(request.getDeviceName() != null ? request.getDeviceName() : userAgent)
                .ipAddress(ipAddress)
                .location("Localhost (Mock)")
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        deviceSessionRepository.save(session);

        // Record history
        LoginHistory history = LoginHistory.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .ipAddress(ipAddress)
                .browser(userAgent)
                .location("Localhost (Mock)")
                .createdAt(LocalDateTime.now())
                .build();
        loginHistoryRepository.save(history);

        // Publish online activity
        UserActivityEvent event = UserActivityEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .activityType("LOGIN")
                .status("ONLINE")
                .details("Device: " + session.getDeviceName())
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend("message.exchange", "user.activity", event);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToDto(user))
                .build();
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request, String ipAddress) {
        String rToken = request.getRefreshToken();
        if (!JwtUtil.validateToken(rToken)) {
            throw new RuntimeException("Expired refresh token");
        }

        Claims claims = JwtUtil.extractClaims(rToken);
        UUID userId = UUID.fromString(claims.getSubject());
        UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));

        DeviceSession session = deviceSessionRepository.findById(sessionId)
                .orElse(null);

        // Refresh Token Rotation / Theft Protection
        if (session == null || !session.isActive() || !passwordEncoder.matches(rToken, session.getToken())) {
            // Re-use detected! Mark all sessions for this user inactive (compromised)
            logSecurityEvent(userId, "ABUSE", ipAddress, "Refresh token reuse detected for session " + sessionId);
            List<DeviceSession> sessions = deviceSessionRepository.findByUserIdAndIsActiveTrue(userId);
            for (DeviceSession s : sessions) {
                s.setActive(false);
            }
            deviceSessionRepository.saveAll(sessions);
            throw new RuntimeException("Unauthorized: Refresh token compromised");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new pair
        UUID newSessionId = UUID.randomUUID();
        String newAccess = JwtUtil.generateAccessToken(user.getId().toString(), user.getUsername(), List.of("ROLE_USER"), newSessionId.toString());
        String newRefresh = JwtUtil.generateRefreshToken(user.getId().toString(), newSessionId.toString());

        // Deactivate old session
        session.setActive(false);
        deviceSessionRepository.save(session);

        // Save new rotated session
        DeviceSession newSession = DeviceSession.builder()
                .id(newSessionId)
                .userId(user.getId())
                .token(passwordEncoder.encode(newRefresh))
                .deviceName(request.getDeviceName() != null ? request.getDeviceName() : session.getDeviceName())
                .ipAddress(ipAddress)
                .location(session.getLocation())
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        deviceSessionRepository.save(newSession);

        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .user(mapToDto(user))
                .build();
    }

    @Transactional
    public void logout(String tokenHeader) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return;
        }
        String token = tokenHeader.substring(7);
        if (!JwtUtil.validateToken(token)) return;

        Claims claims = JwtUtil.extractClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());
        UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));

        DeviceSession session = deviceSessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setActive(false);
            deviceSessionRepository.save(session);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus("OFFLINE");
            userRepository.save(user);

            // Publish logout event
            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .activityType("LOGOUT")
                    .status("OFFLINE")
                    .details("Log out device: " + (session != null ? session.getDeviceName() : "unknown"))
                    .timestamp(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend("message.exchange", "user.activity", event);
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceSessionDto> getActiveSessions(UUID userId) {
        return deviceSessionRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(s -> DeviceSessionDto.builder()
                        .id(s.getId())
                        .deviceName(s.getDeviceName())
                        .ipAddress(s.getIpAddress())
                        .location(s.getLocation())
                        .lastActiveAt(s.getLastActiveAt())
                        .isActive(s.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(UUID sessionId, UUID userId) {
        DeviceSession session = deviceSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setActive(false);
        deviceSessionRepository.save(session);
    }

    private void logSecurityEvent(UUID userId, String eventType, String ipAddress, String details) {
        SecurityEvent event = SecurityEvent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .eventType(eventType)
                .ipAddress(ipAddress)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        securityEventRepository.save(event);
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
