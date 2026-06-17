package com.chatsphere.chat.service;

import com.chatsphere.chat.entity.ScheduledMessage;
import com.chatsphere.chat.repository.ScheduledMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageScheduler {

    private final ScheduledMessageRepository scheduledRepository;
    private final ChatService chatService;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processScheduledMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledMessage> dueMessages = scheduledRepository.findByStatusAndScheduledTimeBefore("PENDING", now);
        
        if (dueMessages.isEmpty()) return;

        log.info("Found {} due scheduled messages to transmit", dueMessages.size());

        for (ScheduledMessage msg : dueMessages) {
            try {
                if (msg.isGroup()) {
                    // Group messaging will be implemented in group service, but we log or proxy here
                    log.warn("Scheduled group messages are handled via group service routes.");
                    msg.setStatus("FAILED");
                } else {
                    String clientMsgId = "SCHED_" + UUID.randomUUID().toString().substring(0, 10);
                    chatService.sendMessage(
                            msg.getSenderId(),
                            "Scheduled Sender",
                            msg.getRecipientId(),
                            clientMsgId,
                            msg.getMessage(),
                            "TEXT",
                            null
                    );
                    msg.setStatus("SENT");
                }
                scheduledRepository.save(msg);
                log.info("Successfully transmitted scheduled message ID: {}", msg.getId());
            } catch (Exception e) {
                log.error("Failed to transmit scheduled message ID: {}", msg.getId(), e);
                msg.setStatus("FAILED");
                scheduledRepository.save(msg);
            }
        }
    }
}
