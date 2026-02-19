package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Message;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.enums.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.createdAt ASC")
    List<Message> findChatBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    List<Message> findByTypeAndReceiverOrderByCreatedAtDesc(MessageType type, User receiver);

    @Query("""
        SELECT m FROM Message m
        WHERE m.type = :type
          AND (
            (m.sender = :user AND m.receiver.role = :adminRole)
            OR
            (m.receiver = :user AND m.sender.role = :adminRole)
          )
        ORDER BY m.createdAt ASC
        """)
    List<Message> findSupportThreadForUser(
        @Param("user") User user,
        @Param("type") MessageType type,
        @Param("adminRole") UserRole adminRole
    );

    @Query("""
        SELECT m FROM Message m
        WHERE m.type = :type
          AND (m.sender.role = :adminRole OR m.receiver.role = :adminRole)
        ORDER BY m.createdAt DESC
        """)
    List<Message> findAllSupportMessagesWithAdminsOrderByCreatedAtDesc(
        @Param("type") MessageType type,
        @Param("adminRole") UserRole adminRole
    );
}

