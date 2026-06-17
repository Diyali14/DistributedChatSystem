package com.chatsphere.group.repository;

import com.chatsphere.group.entity.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, UUID> {
    Optional<GroupInvite> findByInviteCode(String inviteCode);
}
