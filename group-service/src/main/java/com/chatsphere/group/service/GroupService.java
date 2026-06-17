package com.chatsphere.group.service;

import com.chatsphere.common.event.GroupEvent;
import com.chatsphere.common.event.MessageSentEvent;
import com.chatsphere.group.entity.*;
import com.chatsphere.group.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupMessageRepository messageRepository;
    private final GroupInviteRepository inviteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Group createGroup(String name, String description, UUID ownerId) {
        String lockKey = "chatsphere:lock:group:create:" + ownerId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 5, TimeUnit.SECONDS)) {
                try {
                    Group group = Group.builder()
                            .id(UUID.randomUUID())
                            .name(name)
                            .description(description)
                            .ownerId(ownerId)
                            .build();

                    Group saved = groupRepository.save(group);

                    // Add Owner as Member
                    GroupMember member = GroupMember.builder()
                            .id(UUID.randomUUID())
                            .groupId(saved.getId())
                            .userId(ownerId)
                            .role("OWNER")
                            .build();
                    memberRepository.save(member);

                    // Write Outbox Event
                    GroupEvent event = GroupEvent.builder()
                            .groupId(saved.getId())
                            .groupName(saved.getName())
                            .eventType("CREATE")
                            .actorId(ownerId)
                            .userId(ownerId)
                            .role("OWNER")
                            .timestamp(LocalDateTime.now())
                            .build();

                    saveOutbox("Group", saved.getId(), "GROUP_CREATE", event);

                    return saved;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("Could not acquire lock to create group");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create group", e);
        }
    }

    @Transactional
    public void joinGroup(UUID groupId, UUID userId) {
        String lockKey = "chatsphere:lock:group:members:" + groupId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 5, TimeUnit.SECONDS)) {
                try {
                    if (memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
                        return; // Already in group
                    }

                    Group group = groupRepository.findById(groupId)
                            .orElseThrow(() -> new RuntimeException("Group not found"));

                    GroupMember member = GroupMember.builder()
                            .id(UUID.randomUUID())
                            .groupId(groupId)
                            .userId(userId)
                            .role("MEMBER")
                            .build();
                    memberRepository.save(member);

                    // Outbox
                    GroupEvent event = GroupEvent.builder()
                            .groupId(groupId)
                            .groupName(group.getName())
                            .eventType("JOIN")
                            .actorId(userId)
                            .userId(userId)
                            .role("MEMBER")
                            .timestamp(LocalDateTime.now())
                            .build();

                    saveOutbox("Group", groupId, "GROUP_JOIN", event);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to join group", e);
        }
    }

    @Transactional
    public void removeMember(UUID groupId, UUID memberId, UUID actorId) {
        String lockKey = "chatsphere:lock:group:members:" + groupId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 5, TimeUnit.SECONDS)) {
                try {
                    Group group = groupRepository.findById(groupId)
                            .orElseThrow(() -> new RuntimeException("Group not found"));

                    GroupMember actor = memberRepository.findByGroupIdAndUserId(groupId, actorId)
                            .orElseThrow(() -> new RuntimeException("Actor not in group"));

                    GroupMember target = memberRepository.findByGroupIdAndUserId(groupId, memberId)
                            .orElseThrow(() -> new RuntimeException("Target member not in group"));

                    // Validation: actor must be OWNER or ADMIN (or leaving themselves)
                    boolean isSelf = actorId.equals(memberId);
                    boolean isOwner = "OWNER".equals(actor.getRole());
                    boolean isAdmin = "ADMIN".equals(actor.getRole());

                    if (!isSelf && !isOwner && !isAdmin) {
                        throw new RuntimeException("Unauthorized: Insufficient permissions to remove member");
                    }
                    if ("OWNER".equals(target.getRole()) && !isSelf) {
                        throw new RuntimeException("Cannot remove the owner of the group");
                    }

                    memberRepository.delete(target);

                    GroupEvent event = GroupEvent.builder()
                            .groupId(groupId)
                            .groupName(group.getName())
                            .eventType(isSelf ? "LEAVE" : "KICK")
                            .actorId(actorId)
                            .userId(memberId)
                            .timestamp(LocalDateTime.now())
                            .build();

                    saveOutbox("Group", groupId, "GROUP_LEAVE", event);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove member", e);
        }
    }

    @Transactional
    public String generateInviteLink(UUID groupId, UUID creatorId, int maxUses, LocalDateTime expiresAt) {
        GroupMember actor = memberRepository.findByGroupIdAndUserId(groupId, creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not in group"));

        if (!"OWNER".equals(actor.getRole()) && !"ADMIN".equals(actor.getRole())) {
            throw new RuntimeException("Unauthorized: Only owners or admins can create invite links");
        }

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        GroupInvite invite = GroupInvite.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .inviteCode(code)
                .creatorId(creatorId)
                .maxUses(maxUses)
                .usedCount(0)
                .expiresAt(expiresAt)
                .build();

        inviteRepository.save(invite);
        return "/api/groups/join/" + code;
    }

    @Transactional
    public void joinByInviteCode(String inviteCode, UUID userId) {
        GroupInvite invite = inviteRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite code expired");
        }

        if (invite.getMaxUses() > 0 && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new RuntimeException("Invite link reached maximum usage limit");
        }

        invite.setUsedCount(invite.getUsedCount() + 1);
        inviteRepository.save(invite);

        joinGroup(invite.getGroupId(), userId);
    }

    @Transactional
    public GroupMessage sendGroupMessage(UUID senderId, String senderName, UUID groupId, String clientMsgId, String content, String type, UUID replyToId) {
        // Idempotency
        if (messageRepository.existsByClientMessageId(clientMsgId)) {
            return messageRepository.findByGroupIdOrderBySequenceNumberDesc(groupId, PageRequest.of(0, 1))
                    .stream()
                    .filter(m -> m.getClientMessageId().equals(clientMsgId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Duplicate group message"));
        }

        // Verify membership
        if (!memberRepository.existsByGroupIdAndUserId(groupId, senderId)) {
            throw new RuntimeException("Cannot send message: You are not a member of this group");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Atomic group message sequence using Redis
        String seqKey = "chatsphere:seq:group:" + groupId;
        Long nextSeq = redisTemplate.opsForValue().increment(seqKey);
        if (nextSeq == null) nextSeq = 1L;

        GroupMessage gmsg = GroupMessage.builder()
                .id(UUID.randomUUID())
                .clientMessageId(clientMsgId)
                .groupId(groupId)
                .senderId(senderId)
                .message(content)
                .type(type != null ? type : "TEXT")
                .sequenceNumber(nextSeq)
                .replyToId(replyToId)
                .createdAt(LocalDateTime.now())
                .build();

        GroupMessage saved = messageRepository.save(gmsg);

        // Process mentions: scan content for @everyone or @username
        List<String> mentions = parseMentions(content);
        log.info("Parsed group message mentions: {}", mentions);

        // Save Outbox
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(saved.getId())
                .clientMessageId(clientMsgId)
                .senderId(senderId)
                .senderName(senderName)
                .groupId(groupId)
                .message(content)
                .type(saved.getType())
                .isGroup(true)
                .sequenceNumber(nextSeq)
                .createdAt(saved.getCreatedAt())
                .build();

        saveOutbox("GroupMessage", saved.getId(), "GROUP_MESSAGE_SENT", event);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<GroupMessage> getMessages(UUID groupId, UUID userId, int page, int size) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("Unauthorized: Access to group message history denied");
        }
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByGroupIdOrderBySequenceNumberDesc(groupId, pageable);
    }

    private List<String> parseMentions(String message) {
        List<String> list = new ArrayList<>();
        if (message == null) return list;
        if (message.contains("@everyone")) {
            list.add("everyone");
        }
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (!match.equalsIgnoreCase("everyone")) {
                list.add(match);
            }
        }
        return list;
    }

    private void saveOutbox(String aggType, UUID aggId, String eventType, Object payload) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(aggType)
                    .aggregateId(aggId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Outbox save failed for group event", e);
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> getGroupMessageRecipients(UUID groupId, UUID excludeSender) {
        return memberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .filter(userId -> !userId.equals(excludeSender))
                .collect(Collectors.toList());
    }
}

