package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, UUID> {
    boolean existsByUserIdAndBlockedUserId(UUID userId, UUID blockedUserId);
    List<BlockedUser> findByUserId(UUID userId);
}
