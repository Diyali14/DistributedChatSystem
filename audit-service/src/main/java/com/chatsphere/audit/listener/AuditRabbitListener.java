package com.chatsphere.audit.listener;

import com.chatsphere.audit.entity.AuditLog;
import com.chatsphere.audit.repository.AuditLogRepository;
import com.chatsphere.common.event.GroupEvent;
import com.chatsphere.common.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRabbitListener {

    private final AuditLogRepository auditLogRepository;

    @RabbitListener(queues = "audit.queue")
    public void handleUserActivity(Object event) {
        log.info("Audit log consumer received event");

        if (event instanceof UserActivityEvent actEvent) {
            AuditLog logEntry = AuditLog.builder()
                    .id(UUID.randomUUID())
                    .userId(actEvent.getUserId())
                    .action(actEvent.getActivityType())
                    .details(actEvent.getDetails())
                    .ipAddress(actEvent.getIpAddress())
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logEntry);
            log.info("Saved user activity audit entry: {}", actEvent.getActivityType());

        } else if (event instanceof GroupEvent grpEvent) {
            AuditLog logEntry = AuditLog.builder()
                    .id(UUID.randomUUID())
                    .userId(grpEvent.getActorId())
                    .action("GROUP_" + grpEvent.getEventType())
                    .details(String.format("Group: %s (%s). Target User: %s. Role: %s", 
                            grpEvent.getGroupName(), grpEvent.getGroupId(), grpEvent.getUserId(), grpEvent.getRole()))
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logEntry);
            log.info("Saved group activity audit entry: {}", grpEvent.getEventType());
        }
    }
}
