package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.UserNote;
import com.ftn.drumigo.dto.*;
import com.ftn.drumigo.mapper.UserMapper;
import com.ftn.drumigo.mapper.UserNoteMapper;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.AuthService;
import com.ftn.drumigo.service.UserNoteService;
import com.ftn.drumigo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserNoteService userNoteService;
    private final UserNoteMapper userNoteMapper;
    private final AuthService authService;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        User user = userService.update(id, request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @PutMapping("/{id}/block")
    public ResponseEntity<UserResponse> blockUser(@PathVariable Long id) {
        User user = userService.block(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @PutMapping("/{id}/unblock")
    public ResponseEntity<UserResponse> unblockUser(@PathVariable Long id) {
        User user = userService.unblock(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @PostMapping("/{id}/notes")
    public ResponseEntity<UserNoteResponse> createNote(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @Valid @RequestBody UserNoteCreateRequest request) {
        UserNote note = userNoteService.create(id, adminDetails.getUserId(), request);
        return ResponseEntity.status(201).body(userNoteMapper.toResponse(note));
    }
    
    @GetMapping("/{id}/notes")
    public ResponseEntity<List<UserNoteResponse>> getUserNotes(@PathVariable Long id) {
        List<UserNote> notes = userNoteService.getByUser(id);
        List<UserNoteResponse> responses = notes.stream()
            .map(userNoteMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordUpdateRequest request) {
        authService.updatePassword(id, request);
        return ResponseEntity.ok().build();
    }
}

