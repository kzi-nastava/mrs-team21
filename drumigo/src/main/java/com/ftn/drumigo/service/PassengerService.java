package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Passenger;
import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.enums.TokenType;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.dto.auth.request.PassengerRegisterRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ftn.drumigo.util.TokenUtil;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PassengerService {
    
    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final EmailService emailService;
    
    public Passenger register(PassengerRegisterRequest request) {
        String password = request.password();
        String confirm = request.confirmPassword();

        // Check if passwords match
        if (!password.equals(confirm)) {
            throw new ConflictException("Passwords don't match");
        }

        // Validate password strength: at least 6 chars, at least one uppercase and one lowercase
        if (!isValidPassword(password)) {
            throw new BadRequestException("Password must be at least 6 characters and contain at least one uppercase and one lowercase letter");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("User with email " + request.email() + " already exists");
        }
        
        // Create passenger
        Passenger passenger = new Passenger();
        passenger.setName(request.firstName());
        passenger.setSurname(request.lastName());
        passenger.setEmail(request.email());
        passenger.setAddress(request.address());
        passenger.setPhone(request.phoneNumber());
        passenger.setRole(UserRole.PASSENGER);
        passenger.setActive(false); // Not activated yet
        passenger.setBlocked(false);
        passenger.setCreatedAt(Instant.now());
        passenger.setUpdatedAt(Instant.now());
        
        passenger = passengerRepository.save(passenger);
        
        // Create token and send confirmation email
        String token = createActivationToken(passenger);
        emailService.sendActivationEmail(passenger.getEmail(), token);

        return passenger;
    }
    
    public void activate(String token) {
        // Hash the token to find it in DB
        String tokenHash = TokenUtil.hashToken(token);
        
        UserToken userToken = userTokenRepository
            .findByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(
                tokenHash, TokenType.PASSENGER_ACTIVATION, Instant.now())
            .orElseThrow(() -> new BadRequestException("Invalid or expired activation token"));
        
        // Mark token as used
        userToken.setUsedAt(Instant.now());
        userTokenRepository.save(userToken);
        
        // Fetch the Passenger entity directly to avoid proxy casting issues
        Passenger passenger = passengerRepository.findById(userToken.getUser().getId())
            .orElseThrow(() -> new BadRequestException("Passenger not found"));

        // Activate user
        passenger.setActive(true);
        passenger.setUpdatedAt(Instant.now());
        passengerRepository.save(passenger);
    }
    
    private String createActivationToken(Passenger passenger) {
        // Generate token
        String token = UUID.randomUUID().toString();
        String tokenHash = TokenUtil.hashToken(token);
        
        // Create token entity
        UserToken userToken = new UserToken();
        userToken.setUser(passenger);
        userToken.setTokenHash(tokenHash);
        userToken.setType(TokenType.PASSENGER_ACTIVATION);
        userToken.setExpiresAt(Instant.now().plusSeconds(24 * 60 * 60)); // 24 hours
        userToken.setCreatedAt(Instant.now());
        
        userTokenRepository.save(userToken);
        return token;
    }

    // Password must be at least 6 chars, contain at least one uppercase and one lowercase letter
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("(?=.*[a-z])(?=.*[A-Z]).{6,}");
    }
}
