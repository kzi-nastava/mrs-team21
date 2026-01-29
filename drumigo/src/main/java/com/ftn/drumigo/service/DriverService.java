package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.domain.enums.TokenType;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.dto.DriverCreateRequest;
import com.ftn.drumigo.dto.DriverUpdateRequest;
import com.ftn.drumigo.dto.SetPasswordRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.DriverRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.repository.UserTokenRepository;
import com.ftn.drumigo.repository.VehicleRepository;
import com.ftn.drumigo.repository.VehicleTypeRepository;
import com.ftn.drumigo.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverService {
    
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final UserTokenRepository userTokenRepository;
    
    public Driver create(DriverCreateRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("User with email " + request.email() + " already exists");
        }
        
        // Get vehicle type
        VehicleType vehicleType = vehicleTypeRepository.findById(request.vehicleTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + request.vehicleTypeId()));
        
        // Create driver
        Driver driver = new Driver();
        driver.setName(request.name());
        driver.setSurname(request.surname());
        driver.setEmail(request.email());
        driver.setAddress(request.address());
        driver.setPhone(request.phone());
        driver.setRole(UserRole.DRIVER);
        driver.setActive(false); // Not active until password is set
        driver.setActiveDriver(false);
        driver.setBlocked(false);
        driver.setCreatedAt(Instant.now());
        driver.setUpdatedAt(Instant.now());
        
        driver = driverRepository.save(driver);
        
        // Create vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setVehicleType(vehicleType);
        vehicle.setModel(request.vehicleModel());
        vehicle.setLicensePlate(request.vehicleLicensePlate());
        vehicle.setNumSeats(request.vehicleNumSeats());
        vehicle.setBabyFriendly(request.vehicleBabyFriendly() != null ? request.vehicleBabyFriendly() : false);
        vehicle.setPetFriendly(request.vehiclePetFriendly() != null ? request.vehiclePetFriendly() : false);
        vehicle.setAvailable(true);
        
        vehicleRepository.save(vehicle);
        
        return driver;
    }
    
    public Page<Driver> getAll(Pageable pageable) {
        return driverRepository.findAll(pageable);
    }
    
    public Driver getById(Long id) {
        return driverRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }
    
    public Driver update(Long id, DriverUpdateRequest request) {
        Driver driver = getById(id);
        
        // Check email uniqueness if changed
        if (request.email() != null && !request.email().equals(driver.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("User with email " + request.email() + " already exists");
            }
            driver.setEmail(request.email());
        }
        
        if (request.name() != null) {
            driver.setName(request.name());
        }
        if (request.surname() != null) {
            driver.setSurname(request.surname());
        }
        if (request.address() != null) {
            driver.setAddress(request.address());
        }
        if (request.phone() != null) {
            driver.setPhone(request.phone());
        }
        driver.setUpdatedAt(Instant.now());
        
        return driverRepository.save(driver);
    }
    
    public String createActivationToken(Long driverId) {
        Driver driver = getById(driverId);
        
        // Generate token
        String token = UUID.randomUUID().toString();
        String tokenHash = com.ftn.drumigo.util.TokenUtil.hashToken(token);
        
        // Create token entity (expires in 24 hours)
        UserToken userToken = new UserToken();
        userToken.setUser(driver);
        userToken.setTokenHash(tokenHash);
        userToken.setType(TokenType.DRIVER_SET_PASSWORD);
        userToken.setExpiresAt(Instant.now().plusSeconds(24 * 60 * 60)); // 24 hours
        userToken.setUsedAt(null);
        userToken.setCreatedAt(Instant.now());
        
        userTokenRepository.save(userToken);
        
        return token;
    }
    
    public void setPassword(String token, SetPasswordRequest request) {
        String tokenHash = com.ftn.drumigo.util.TokenUtil.hashToken(token);
        Instant now = Instant.now();
        
        UserToken userToken = userTokenRepository
            .findByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(tokenHash, TokenType.DRIVER_SET_PASSWORD, now)
            .orElseThrow(() -> new BadRequestException("Invalid or expired activation token"));
        
        // Note: usedAt check is redundant since query already filters by usedAtIsNull, but kept for clarity
        
        // Set password hash (for KT1, simple hash; in production use BCrypt)
        String passwordHash = PasswordUtil.hashPassword(request.password());

        Driver driver = (Driver) userToken.getUser();
        driver.setPasswordHash(passwordHash);
        driver.setUpdatedAt(Instant.now());
        
        driverRepository.save(driver);
        
        // Mark token as used
        userToken.setUsedAt(Instant.now());
        userTokenRepository.save(userToken);
    }
    
    public Driver updateDriverState(Long id, Boolean activeDriver) {
        Driver driver = getById(id);
        driver.setActiveDriver(activeDriver);
        driver.setLastStateChangeAt(Instant.now());
        return driverRepository.save(driver);
    }
}
