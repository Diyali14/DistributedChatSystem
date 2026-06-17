package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.ConversationSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ConversationSequenceRepository extends JpaRepository<ConversationSequence, UUID> {
}
