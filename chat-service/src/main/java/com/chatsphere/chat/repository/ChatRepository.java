package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {
    
    @Query("SELECT c FROM Chat c WHERE c.isDeleted = false AND " +
           "((c.senderId = :u1 AND c.receiverId = :u2) OR (c.senderId = :u2 AND c.receiverId = :u1)) " +
           "ORDER BY c.sequenceNumber DESC")
    List<Chat> findConversationMessages(@Param("u1") UUID u1, @Param("u2") UUID u2, Pageable pageable);
    
    boolean existsByClientMessageId(String clientMessageId);

    @Query(value = "SELECT * FROM chats WHERE is_deleted = false AND " +
                   "((sender_id = :u1 AND receiver_id = :u2) OR (sender_id = :u2 AND receiver_id = :u1)) AND " +
                   "message ILIKE CONCAT('%', :query, '%') ORDER BY sequence_number DESC", nativeQuery = true)
    List<Chat> searchMessagesInConversation(@Param("u1") UUID u1, @Param("u2") UUID u2, @Param("query") String query);
}
