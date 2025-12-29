package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.User;
import com.ftn.drumigo.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getSurname(),
            user.getEmail(),
            user.getAddress(),
            user.getPhone(),
            user.getProfilePictureUrl(),
            user.getBlocked(),
            user.getRole(),
            user.getActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}

