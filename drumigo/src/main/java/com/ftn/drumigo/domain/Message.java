package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.enums.MessageType;
import com.ftn.drumigo.domain.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

