package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.DraftMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DraftMessageRepository extends JpaRepository<DraftMessage, UUID> {
    Optional<DraftMessage> findByUserIdAndRecipientIdAndIsGroup(UUID userId, UUID recipientId, boolean isGroup);
}
