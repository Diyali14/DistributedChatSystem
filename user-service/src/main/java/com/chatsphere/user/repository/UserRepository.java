package com.chatsphere.user.repository;

import com.chatsphere.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    @Query(value = "SELECT * FROM users WHERE enabled = true AND (" +
                   "username ILIKE CONCAT('%', :query, '%') OR " +
                   "email ILIKE CONCAT('%', :query, '%') OR " +
                   "bio ILIKE CONCAT('%', :query, '%'))", nativeQuery = true)
    List<User> searchUsers(@Param("query") String query);
}
