package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Passenger;
import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.enums.TokenType;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.dto.PassengerCreateRequest;
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
    
    public Passenger create(PassengerCreateRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("User with email " + request.email() + " already exists");
        }
        
        // Create passenger
        Passenger passenger = new Passenger();
        passenger.setName(request.name());
        passenger.setSurname(request.surname());
        passenger.setEmail(request.email());
        passenger.setAddress(request.address());
        passenger.setPhone(request.phone());
        passenger.setRole(UserRole.PASSENGER);
        passenger.setActive(false); // Not activated yet
        passenger.setBlocked(false);
        passenger.setCreatedAt(Instant.now());
        passenger.setUpdatedAt(Instant.now());
        
        passenger = passengerRepository.save(passenger);
        
        // Create activation token
        createActivationToken(passenger);
        
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
        
        // Activate user
        Passenger passenger = (Passenger) userToken.getUser();
        passenger.setActive(true);
        passenger.setUpdatedAt(Instant.now());
        passengerRepository.save(passenger);
    }
    
    private void createActivationToken(Passenger passenger) {
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
    }
    
}

