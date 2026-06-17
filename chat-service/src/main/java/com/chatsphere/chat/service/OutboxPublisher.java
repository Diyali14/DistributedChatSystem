package com.chatsphere.chat.service;

import com.chatsphere.chat.entity.OutboxEvent;
import com.chatsphere.chat.repository.OutboxEventRepository;
import com.chatsphere.common.event.MessageSentEvent;
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

        log.debug("Found {} pending outbox events to publish", events.size());

        for (OutboxEvent event : events) {
            try {
                if ("MESSAGE_SENT".equals(event.getEventType())) {
                    MessageSentEvent payload = objectMapper.readValue(event.getPayload(), MessageSentEvent.class);
                    // Publish to RabbitMQ exchange
                    rabbitTemplate.convertAndSend("message.exchange", "message.sent", payload);
                } else if ("MESSAGE_DELETED".equals(event.getEventType())) {
                    rabbitTemplate.convertAndSend("message.exchange", "message.deleted", event.getPayload());
                }

                // Mark processed
                event.setStatus("PROCESSED");
                outboxEventRepository.save(event);
                log.info("Published outbox event id: {} type: {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                // We keep status as PENDING to retry on next cycle
            }
        }
    }
}
