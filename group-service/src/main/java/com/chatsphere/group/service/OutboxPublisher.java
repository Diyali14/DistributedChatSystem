package com.chatsphere.group.service;

import com.chatsphere.common.event.GroupEvent;
import com.chatsphere.common.event.MessageSentEvent;
import com.chatsphere.group.entity.OutboxEvent;
import com.chatsphere.group.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (events.isEmpty()) return;

        for (OutboxEvent event : events) {
            try {
                if ("GROUP_MESSAGE_SENT".equals(event.getEventType())) {
                    MessageSentEvent payload = objectMapper.readValue(event.getPayload(), MessageSentEvent.class);
                    rabbitTemplate.convertAndSend("message.exchange", "group.message.sent", payload);
                } else if (event.getEventType().startsWith("GROUP_")) {
                    GroupEvent payload = objectMapper.readValue(event.getPayload(), GroupEvent.class);
                    rabbitTemplate.convertAndSend("message.exchange", "group.activity", payload);
                }

                event.setStatus("PROCESSED");
                outboxEventRepository.save(event);
                log.info("Published group outbox event id: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to publish group outbox event: {}", event.getId(), e);
            }
        }
    }
}
