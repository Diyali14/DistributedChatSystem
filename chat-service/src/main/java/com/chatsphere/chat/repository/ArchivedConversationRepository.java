package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.ArchivedConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ArchivedConversationRepository extends JpaRepository<ArchivedConversation, UUID> {
    boolean existsByUserIdAndConversationId(UUID userId, UUID conversationId);
    List<ArchivedConversation> findByUserId(UUID userId);
}
