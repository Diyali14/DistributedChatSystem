package com.chatsphere.group.repository;

import com.chatsphere.group.entity.GroupMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {
    List<GroupMessage> findByGroupIdOrderBySequenceNumberDesc(UUID groupId, Pageable pageable);
    boolean existsByClientMessageId(String clientMessageId);
}
