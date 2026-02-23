package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Message;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.enums.MessageType;
import com.ftn.drumigo.dto.SupportConversationSummaryResponse;
import com.ftn.drumigo.dto.SupportMessageCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.MessageRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    public Message createSupportMessage(Long senderId, String senderRole, SupportMessageCreateRequest request) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + senderId));

        UserRole role = sender.getRole();
        if (role == UserRole.ADMIN) {
            return createMessage(sender, resolveAdminReceiver(request), request.content());
        }
        if (role == UserRole.PASSENGER || role == UserRole.DRIVER) {
            User adminReceiver = resolveDefaultAdmin();
            return createMessage(sender, adminReceiver, request.content());
        }

        throw new BadRequestException("Unsupported role for support messaging: " + senderRole);
    }

    public List<Message> getMySupportChat(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Admins should use conversation-specific support endpoints.");
        }

        return messageRepository.findSupportThreadForUser(user, MessageType.SUPPORT, UserRole.ADMIN);
    }

    public List<SupportConversationSummaryResponse> getSupportConversationsForAdmin() {
        List<Message> messages = messageRepository.findAllSupportMessagesWithAdminsOrderByCreatedAtDesc(
            MessageType.SUPPORT,
            UserRole.ADMIN
        );

        Map<Long, SupportConversationSummaryResponse> summaries = new LinkedHashMap<>();
        for (Message message : messages) {
            User sender = message.getSender();
            User receiver = message.getReceiver();
            boolean senderIsAdmin = sender.getRole() == UserRole.ADMIN;
            boolean receiverIsAdmin = receiver.getRole() == UserRole.ADMIN;

            // Ignore malformed rows where both users are admin or both are non-admin.
            if (senderIsAdmin == receiverIsAdmin) {
                continue;
            }

            User conversationUser = senderIsAdmin ? receiver : sender;
            summaries.putIfAbsent(
                conversationUser.getId(),
                new SupportConversationSummaryResponse(
                    conversationUser.getId(),
                    conversationUser.getName(),
                    conversationUser.getSurname(),
                    message.getContent(),
                    message.getCreatedAt(),
                    sender.getId(),
                    sender.getRole().name()
                )
            );
        }

        return new ArrayList<>(summaries.values());
    }

    public List<Message> getSupportConversationForAdmin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Admin-to-admin support conversations are not allowed.");
        }

        return messageRepository.findSupportThreadForUser(user, MessageType.SUPPORT, UserRole.ADMIN);
    }

    public List<Message> getChatHistory(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId1));
        
        User user2 = userRepository.findById(userId2)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId2));
        
        return messageRepository.findChatBetweenUsers(user1, user2);
    }

    private Message createMessage(User sender, User receiver, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setType(MessageType.SUPPORT);
        return messageRepository.save(message);
    }

    private User resolveAdminReceiver(SupportMessageCreateRequest request) {
        if (request.receiverId() == null) {
            throw new BadRequestException("Receiver ID is required for admin support messages.");
        }

        User receiver = userRepository.findById(request.receiverId())
            .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.receiverId()));
        if (receiver.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Admin cannot send support messages to another admin.");
        }
        return receiver;
    }

    private User resolveDefaultAdmin() {
        return userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)
            .orElseThrow(() -> new BadRequestException("No admin is available for support."));
    }
        
}

