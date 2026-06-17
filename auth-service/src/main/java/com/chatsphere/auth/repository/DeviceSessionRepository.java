package com.chatsphere.auth.repository;

import com.chatsphere.auth.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {
    Optional<DeviceSession> findByToken(String token);
    List<DeviceSession> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<DeviceSession> findByIdAndUserId(UUID id, UUID userId);
}
