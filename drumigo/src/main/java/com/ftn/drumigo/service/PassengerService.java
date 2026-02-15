package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.enums.TokenType;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.dto.auth.request.PassengerRegisterRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ftn.drumigo.util.TokenUtil;
import com.ftn.drumigo.util.PasswordUtil;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PassengerService {
    
    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final RideRepository rideRepository;

    private final EmailService emailService;
    
    public Passenger register(PassengerRegisterRequest request) {
        String password = request.password();
        String confirm = request.confirmPassword();

        // Check if passwords match
        if (!password.equals(confirm)) {
            throw new BadRequestException("Passwords don't match");
        }

        // Validate password strength: 6-64 chars, at least one uppercase, one lowercase, and one digit or special char
        if (!isValidPassword(password)) {
            throw new BadRequestException(
                "Password must be 6-64 characters and contain at least one uppercase letter, one lowercase letter, and one digit or special character"
            );
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

        passenger.setBlocked(false);
        passenger.setActive(false);
        passenger.setCreatedAt(Instant.now());
        passenger.setUpdatedAt(Instant.now());
        passenger.setPasswordHash(PasswordUtil.hashPassword(password)); // Hash password

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

        passenger.setActive(true);

        // Update timestamp
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

    // Password must be 6-64 chars, contain at least one uppercase letter, one lowercase letter, and one digit or special character
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9\\W]).{6,64}");
    }

    public Page<Ride> getPassengerRideHistory(Long passengerId, Instant from, Instant to,
                                              List<RideStatus> statuses, Boolean hasPanic, Pageable pageable) {
        final Instant fromFinal = from == null ? Instant.ofEpochMilli(0) : from;
        final Instant toFinal = to == null ? Instant.now() : to;

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));

        List<RideStatus> statusFilter = (statuses == null || statuses.isEmpty()) ? null : statuses;

        return rideRepository.findPassengerHistory(
                passengerId,
                passenger.getEmail(),
                fromFinal,
                toFinal,
                statusFilter,
                hasPanic,
                pageable
        );
    }
}
