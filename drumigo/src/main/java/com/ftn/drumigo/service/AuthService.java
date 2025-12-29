package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Driver;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.User;
import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.enums.TokenType;
import com.ftn.drumigo.dto.LoginRequest;
import com.ftn.drumigo.dto.LoginResponse;
import com.ftn.drumigo.dto.PasswordUpdateRequest;
import com.ftn.drumigo.dto.ResetPasswordConfirmRequest;
import com.ftn.drumigo.dto.ResetPasswordRequestRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.DriverRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ftn.drumigo.util.TokenUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        
        // Check if user is active
        if (!user.getActive()) {
            throw new BadRequestException("Account is not activated");
        }
        
        // Check if user is blocked
        if (user.getBlocked()) {
            throw new BadRequestException("Account is blocked");
        }
        
        // Verify password (CRUD-first: simple hash comparison)
        String passwordHash = hashPassword(request.password());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        
        // If user is a driver, mark as active driver (spec 2.2.1)
        if (user instanceof Driver driver) {
            driver.setActiveDriver(true);
            driver.setLastStateChangeAt(Instant.now());
            driverRepository.save(driver);
        }
        
        // Generate token (placeholder for JWT later)
        String token = "token_" + UUID.randomUUID().toString();
        
        return new LoginResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            token
        );
    }
    
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // If user is a driver, check if they have an active ride (spec 2.2.1)
        if (user instanceof Driver driver) {
            // Check if driver has an ACTIVE ride
            Ride activeRide = rideRepository.findByDriverAndStatus((Driver) user, RideStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElse(null);
            
            if (activeRide != null) {
                throw new BadRequestException("Cannot logout while having an active ride");
            }
            
            driver.setActiveDriver(false);
            driver.setLastStateChangeAt(Instant.now());
            driverRepository.save(driver);
        }
    }
    
    public void requestPasswordReset(ResetPasswordRequestRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));
        
        // Generate reset token
        String token = UUID.randomUUID().toString();
        String tokenHash = TokenUtil.hashToken(token);
        
        // Create token entity
        UserToken userToken = new UserToken();
        userToken.setUser(user);
        userToken.setTokenHash(tokenHash);
        userToken.setType(TokenType.PASSWORD_RESET);
        userToken.setExpiresAt(Instant.now().plusSeconds(60 * 60)); // 60 minutes
        userToken.setCreatedAt(Instant.now());
        
        userTokenRepository.save(userToken);
        
        // In production, send email with token
        // For KT1, token is stored in DB and can be retrieved via admin endpoint if needed
    }
    
    public void confirmPasswordReset(ResetPasswordConfirmRequest request) {
        String tokenHash = TokenUtil.hashToken(request.token());
        
        UserToken userToken = userTokenRepository
            .findByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(
                tokenHash, TokenType.PASSWORD_RESET, Instant.now())
            .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        
        // Mark token as used
        userToken.setUsedAt(Instant.now());
        userTokenRepository.save(userToken);
        
        // Update password
        User user = userToken.getUser();
        user.setPasswordHash(hashPassword(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
    
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Verify current password
        String currentPasswordHash = hashPassword(request.currentPassword());
        if (!currentPasswordHash.equals(user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(hashPassword(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
}

