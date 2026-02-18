package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.enums.TokenType;
import com.ftn.drumigo.dto.auth.request.LoginRequest;
import com.ftn.drumigo.dto.auth.response.LoginResponse;
import com.ftn.drumigo.dto.PasswordUpdateRequest;
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
import com.ftn.drumigo.util.PasswordUtil;
import com.ftn.drumigo.util.JwtUtil;
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
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final PasswordUtil passwordUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        
        // Check if user is blocked
        if (user.getBlocked()) {
            throw new BadRequestException("Account is blocked");
        }

        // Check if account is activated
        if (!user.getActive()) {
            throw new BadRequestException("Account is not activated");
        }
        
        // Verify password (CRUD-first: simple hash comparison)
        String passwordHash = PasswordUtil.hashPassword(request.password());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        
        // If user is a driver, mark as active driver
        if (user instanceof Driver driver) {
            driver.setActiveDriver(true);
            driver.setLastStateChangeAt(Instant.now());
            driverRepository.save(driver);
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user);

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
            Ride activeRide = rideRepository.findByDriverAndStatus(driver, RideStatus.ACTIVE)
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
    
    public void requestPasswordReset(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Generate reset token
        String token = UUID.randomUUID().toString();
        String tokenHash = TokenUtil.hashToken(token);
        
        // Create token entity
        UserToken userToken = new UserToken();
        userToken.setUser(user);
        userToken.setTokenHash(tokenHash);
        userToken.setType(TokenType.PASSWORD_RESET);
        userToken.setExpiresAt(Instant.now().plusSeconds(30 * 60)); // 30 minutes
        userToken.setCreatedAt(Instant.now());
        
        userTokenRepository.save(userToken);

        // Send email to user
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }
    
    public void resetPassword(Long userId, String token, String newPassword) {
        String tokenHash = TokenUtil.hashToken(token);
        Instant now = Instant.now();

        UserToken userToken = userTokenRepository
            .findByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(
                tokenHash, TokenType.PASSWORD_RESET, now)
            .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        // Verify that the authenticated user matches the token's user
        if (!userId.equals(userToken.getUser().getId())) {
            throw new BadRequestException("You can only reset your own password");
        }

        // Validate new password
        if (!passwordUtil.isValid(newPassword)) {
            throw new BadRequestException("New password has to have at least 6 characters, one capital character and one number.");
        }

        // Mark token as used
        userToken.setUsedAt(now);
        userTokenRepository.save(userToken);
        
        // Update password
        User user = userToken.getUser();
        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        user.setUpdatedAt(now);
        userRepository.save(user);
    }
    
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Verify current password
        String currentPasswordHash = PasswordUtil.hashPassword(request.currentPassword());
        if (!currentPasswordHash.equals(user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password
        if (!passwordUtil.isValid(request.newPassword())) {
            throw new BadRequestException("New password has to have at least 6 characters, one capital character and one number.");
        }

        // Update password
        user.setPasswordHash(PasswordUtil.hashPassword(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

}
