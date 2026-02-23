package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Message;
import com.ftn.drumigo.dto.SupportConversationSummaryResponse;
import com.ftn.drumigo.dto.SupportMessageCreateRequest;
import com.ftn.drumigo.dto.SupportMessageResponse;
import com.ftn.drumigo.mapper.MessageMapper;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    private final MessageMapper messageMapper;
    
    @PostMapping("/messages")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<SupportMessageResponse> createSupportMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SupportMessageCreateRequest request) {
        Message message = messageService.createSupportMessage(
            userDetails.getUserId(),
            userDetails.getRole(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toResponse(message));
    }

    @GetMapping("/my-chat")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<List<SupportMessageResponse>> getMySupportChat(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Message> messages = messageService.getMySupportChat(userDetails.getUserId());
        List<SupportMessageResponse> responses = messages.stream()
            .map(messageMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportConversationSummaryResponse>> getSupportConversations() {
        return ResponseEntity.ok(messageService.getSupportConversationsForAdmin());
    }

    @GetMapping("/conversations/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportMessageResponse>> getSupportConversationByUserId(
        @PathVariable Long userId
    ) {
        List<Message> messages = messageService.getSupportConversationForAdmin(userId);
        List<SupportMessageResponse> responses = messages.stream()
            .map(messageMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/chats/{otherUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportMessageResponse>> getChatHistoryAlias(
        @PathVariable Long otherUserId
    ) {
        List<Message> messages = messageService.getSupportConversationForAdmin(otherUserId);
        List<SupportMessageResponse> responses = messages.stream()
            .map(messageMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

