package com.chatsphere.chat.repository;

import com.chatsphere.chat.entity.ScheduledMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, UUID> {
    List<ScheduledMessage> findByStatusAndScheduledTimeBefore(String status, LocalDateTime time);
}
