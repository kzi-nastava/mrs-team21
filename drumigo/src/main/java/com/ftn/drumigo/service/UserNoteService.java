package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Admin;
import com.ftn.drumigo.domain.User;
import com.ftn.drumigo.domain.UserNote;
import com.ftn.drumigo.dto.UserNoteCreateRequest;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.AdminRepository;
import com.ftn.drumigo.repository.UserNoteRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserNoteService {
    
    private final UserNoteRepository userNoteRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    
    public UserNote create(Long userId, Long adminId, UserNoteCreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        
        UserNote note = new UserNote();
        note.setUser(user);
        note.setAdmin(admin);
        note.setNote(request.note());
        
        return userNoteRepository.save(note);
    }
    
    public List<UserNote> getByUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return userNoteRepository.findByUser(user);
    }
}

