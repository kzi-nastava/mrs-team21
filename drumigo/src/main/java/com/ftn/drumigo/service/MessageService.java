package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Message;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.enums.MessageType;
import com.ftn.drumigo.dto.SupportMessageCreateRequest;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.MessageRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a support message.
     * Security: Sender ID is derived from authentication, not from client input.
     *
     * @param senderId the authenticated sender's ID (from JWT token)
     * @param request the request containing receiverId and content
     * @return the created message
     * @throws ResourceNotFoundException if sender or receiver not found
     */
    public Message createSupportMessage(Long senderId, SupportMessageCreateRequest request) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + senderId));

        User receiver = userRepository.findById(request.receiverId())
            .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.receiverId()));
        
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.content());
        message.setType(MessageType.SUPPORT);
        
        return messageRepository.save(message);
    }
    
    public List<Message> getChatHistory(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId1));
        
        User user2 = userRepository.findById(userId2)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId2));
        
        return messageRepository.findChatBetweenUsers(user1, user2);
    }
}

