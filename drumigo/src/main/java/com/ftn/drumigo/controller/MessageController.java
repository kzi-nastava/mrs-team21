package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Message;
import com.ftn.drumigo.dto.SupportMessageCreateRequest;
import com.ftn.drumigo.dto.SupportMessageResponse;
import com.ftn.drumigo.mapper.MessageMapper;
import com.ftn.drumigo.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SupportMessageResponse> createSupportMessage(
            @Valid @RequestBody SupportMessageCreateRequest request) {
        Message message = messageService.createSupportMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toResponse(message));
    }
    
    @GetMapping("/chats/{userId}")
    public ResponseEntity<List<SupportMessageResponse>> getChatHistory(
            @PathVariable Long userId,
            @RequestParam Long otherUserId) {
        List<Message> messages = messageService.getChatHistory(userId, otherUserId);
        List<SupportMessageResponse> responses = messages.stream()
            .map(messageMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

