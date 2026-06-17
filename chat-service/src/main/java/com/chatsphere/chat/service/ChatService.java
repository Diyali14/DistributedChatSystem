package com.chatsphere.chat.service;

import com.chatsphere.chat.entity.*;
import com.chatsphere.chat.repository.*;
import com.chatsphere.common.dto.UserDto;
import com.chatsphere.common.event.MessageSentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ConversationSequenceRepository sequenceRepository;
    private final MessageReceiptRepository receiptRepository;
    private final DraftMessageRepository draftRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final ReportRepository reportRepository;
    private final ArchivedConversationRepository archivedRepository;
    private final ScheduledMessageRepository scheduledRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public UUID getConversationId(UUID u1, UUID u2) {
        String sorted = u1.compareTo(u2) < 0 ? 
                u1.toString() + "_" + u2.toString() : 
                u2.toString() + "_" + u1.toString();
        return UUID.nameUUIDFromBytes(sorted.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public Chat sendMessage(UUID senderId, String senderName, UUID receiverId, String clientMsgId, String content, String type, UUID replyToId) {
        // 1. Idempotency Check
        if (chatRepository.existsByClientMessageId(clientMsgId)) {
            log.info("Duplicate message detected (Idempotency): {}", clientMsgId);
            return chatRepository.findConversationMessages(senderId, receiverId, PageRequest.of(0, 1))
                    .stream()
                    .filter(c -> c.getClientMessageId().equals(clientMsgId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Message already exists but not found"));
        }

        // 2. Block Check
        if (blockedUserRepository.existsByUserIdAndBlockedUserId(receiverId, senderId)) {
            throw new RuntimeException("Cannot send message: You are blocked by this user");
        }
        if (blockedUserRepository.existsByUserIdAndBlockedUserId(senderId, receiverId)) {
            throw new RuntimeException("Cannot send message: You have blocked this user");
        }

        UUID conversationId = getConversationId(senderId, receiverId);
        String lockKey = "chatsphere:lock:conversation:" + conversationId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Acquire lock with 5 seconds lease time
            if (lock.tryLock(5, 5, TimeUnit.SECONDS)) {
                try {
                    // 3. Message Sequence Generator
                    ConversationSequence seq = sequenceRepository.findById(conversationId)
                            .orElse(ConversationSequence.builder().conversationId(conversationId).lastSequenceNumber(0L).build());

                    long nextSeq = seq.getLastSequenceNumber() + 1;
                    seq.setLastSequenceNumber(nextSeq);
                    sequenceRepository.save(seq);

                    // 4. Save Chat (AES Encryption applied automatically via AesEncryptor Converter)
                    Chat chat = Chat.builder()
                            .id(UUID.randomUUID())
                            .clientMessageId(clientMsgId)
                            .senderId(senderId)
                            .receiverId(receiverId)
                            .message(content)
                            .type(type != null ? type : "TEXT")
                            .sequenceNumber(nextSeq)
                            .replyToId(replyToId)
                            .createdAt(LocalDateTime.now())
                            .build();

                    Chat savedChat = chatRepository.save(chat);

                    // Create Read Receipt
                    MessageReceipt receipt = MessageReceipt.builder()
                            .id(UUID.randomUUID())
                            .messageId(savedChat.getId())
                            .userId(receiverId)
                            .status("SENT")
                            .updatedAt(LocalDateTime.now())
                            .build();
                    receiptRepository.save(receipt);

                    // 5. Transactional Outbox Event Creation
                    MessageSentEvent event = MessageSentEvent.builder()
                            .messageId(savedChat.getId())
                            .clientMessageId(clientMsgId)
                            .senderId(senderId)
                            .senderName(senderName)
                            .receiverId(receiverId)
                            .message(content) // Shared plain text in event bus for notification preview
                            .type(savedChat.getType())
                            .isGroup(false)
                            .sequenceNumber(nextSeq)
                            .createdAt(savedChat.getCreatedAt())
                            .build();

                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .id(UUID.randomUUID())
                            .aggregateType("Chat")
                            .aggregateId(savedChat.getId())
                            .eventType("MESSAGE_SENT")
                            .payload(objectMapper.writeValueAsString(event))
                            .status("PENDING")
                            .createdAt(LocalDateTime.now())
                            .build();

                    outboxEventRepository.save(outboxEvent);

                    // Remove draft if any
                    draftRepository.findByUserIdAndRecipientIdAndIsGroup(senderId, receiverId, false)
                            .ifPresent(draftRepository::delete);

                    return savedChat;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("Could not acquire lock for conversation: " + conversationId);
            }
        } catch (Exception e) {
            log.error("Error sending message: ", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public List<Chat> getMessages(UUID senderId, UUID receiverId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatRepository.findConversationMessages(senderId, receiverId, pageable);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Chat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!chat.getSenderId().equals(userId)) {
            throw new RuntimeException("Cannot delete someone else's message");
        }

        chat.setDeleted(true);
        chat.setDeletedAt(LocalDateTime.now());
        chatRepository.save(chat);

        // Notify downstream services of delete action via Outbox
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("Chat")
                    .aggregateId(chat.getId())
                    .eventType("MESSAGE_DELETED")
                    .payload(String.format("{\"messageId\":\"%s\",\"senderId\":\"%s\",\"receiverId\":\"%s\"}", 
                            chat.getId(), chat.getSenderId(), chat.getReceiverId()))
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Outbox error on message delete: ", e);
        }
    }

    @Transactional
    public void saveDraft(UUID userId, UUID recipientId, boolean isGroup, String content) {
        DraftMessage draft = draftRepository.findByUserIdAndRecipientIdAndIsGroup(userId, recipientId, isGroup)
                .orElse(DraftMessage.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .recipientId(recipientId)
                        .isGroup(isGroup)
                        .build());
        draft.setContent(content);
        draft.setUpdatedAt(LocalDateTime.now());
        draftRepository.save(draft);
    }

    @Transactional(readOnly = true)
    public String getDraft(UUID userId, UUID recipientId, boolean isGroup) {
        return draftRepository.findByUserIdAndRecipientIdAndIsGroup(userId, recipientId, isGroup)
                .map(DraftMessage::getContent)
                .orElse("");
    }

    @Transactional
    public void blockUser(UUID userId, UUID blockUserId) {
        if (blockedUserRepository.existsByUserIdAndBlockedUserId(userId, blockUserId)) return;
        BlockedUser blockedUser = BlockedUser.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .blockedUserId(blockUserId)
                .createdAt(LocalDateTime.now())
                .build();
        blockedUserRepository.save(blockedUser);
    }

    @Transactional
    public void unblockUser(UUID userId, UUID blockUserId) {
        blockedUserRepository.findByUserId(userId).stream()
                .filter(b -> b.getBlockedUserId().equals(blockUserId))
                .findFirst()
                .ifPresent(blockedUserRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<UUID> getBlockedUserIds(UUID userId) {
        return blockedUserRepository.findByUserId(userId).stream()
                .map(BlockedUser::getBlockedUserId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void reportUser(UUID reporterId, UUID reportedUserId, String reason) {
        Report report = Report.builder()
                .id(UUID.randomUUID())
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reason(reason)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        reportRepository.save(report);
    }

    @Transactional
    public void archiveConversation(UUID userId, UUID conversationId, boolean isGroup) {
        if (archivedRepository.existsByUserIdAndConversationId(userId, conversationId)) return;
        ArchivedConversation archived = ArchivedConversation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .conversationId(conversationId)
                .isGroup(isGroup)
                .createdAt(LocalDateTime.now())
                .build();
        archivedRepository.save(archived);
    }

    @Transactional
    public void unarchiveConversation(UUID userId, UUID conversationId) {
        archivedRepository.findByUserId(userId).stream()
                .filter(a -> a.getConversationId().equals(conversationId))
                .findFirst()
                .ifPresent(archivedRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<UUID> getArchivedConversationIds(UUID userId) {
        return archivedRepository.findByUserId(userId).stream()
                .map(ArchivedConversation::getConversationId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void scheduleMessage(UUID senderId, UUID recipientId, boolean isGroup, String content, LocalDateTime scheduledTime) {
        ScheduledMessage scheduled = ScheduledMessage.builder()
                .id(UUID.randomUUID())
                .senderId(senderId)
                .recipientId(recipientId)
                .isGroup(isGroup)
                .message(content)
                .scheduledTime(scheduledTime)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        scheduledRepository.save(scheduled);
    }

    @Transactional(readOnly = true)
    public byte[] exportConversationTxt(UUID u1, UUID u2) {
        List<Chat> chats = chatRepository.findConversationMessages(u1, u2, PageRequest.of(0, 1000));
        StringBuilder sb = new StringBuilder();
        sb.append("--- Conversation History Export ---\n");
        for (int i = chats.size() - 1; i >= 0; i--) {
            Chat c = chats.get(i);
            sb.append("[").append(c.getCreatedAt()).append("] ")
              .append(c.getSenderId()).append(": ")
              .append(c.isDeleted() ? "[DELETED]" : c.getMessage())
              .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void updateReceipt(UUID messageId, UUID userId, String status) {
        receiptRepository.findByMessageId(messageId).stream()
                .filter(r -> r.getUserId().equals(userId))
                .findFirst()
                .ifPresentOrElse(r -> {
                    r.setStatus(status);
                    r.setUpdatedAt(LocalDateTime.now());
                    receiptRepository.save(r);
                }, () -> {
                    MessageReceipt r = MessageReceipt.builder()
                            .id(UUID.randomUUID())
                            .messageId(messageId)
                            .userId(userId)
                            .status(status)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    receiptRepository.save(r);
                });
    }
}
