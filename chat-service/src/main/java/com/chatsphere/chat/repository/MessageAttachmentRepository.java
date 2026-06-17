package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {
    List<MessageAttachment> findByMessageId(UUID messageId);
    List<MessageAttachment> findByGroupMessageId(UUID groupMessageId);
}
