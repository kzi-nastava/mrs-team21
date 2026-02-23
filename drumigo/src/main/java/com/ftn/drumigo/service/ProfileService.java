package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.profile.request.ProfileUpdateRequest;
import com.ftn.drumigo.dto.profile.response.ActiveHoursResponse;
import com.ftn.drumigo.dto.profile.response.ProfileResponse;
import com.ftn.drumigo.dto.profile.response.VehicleInfoResponse;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private static final long MAX_PICTURE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Value("${app.uploads.profile-dir:uploads/profile}")
    private String profileUploadDir;

    private static final int MAX_DRIVING_HOURS_PER_24H = 8;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setSurname(user.getSurname());
        response.setEmail(user.getEmail());
        response.setAddress(user.getAddress());
        response.setPhone(user.getPhone());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setBlocked(user.getBlocked());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        // Add driver-specific fields if user is a driver
        if (user instanceof Driver driver) {
            response.setActiveDriver(driver.getActiveDriver());
            response.setIsBusy(driver.getBusy());
            response.setLastStateChangeAt(driver.getLastStateChangeAt());
            
            // Add vehicle info if driver has a vehicle
            Vehicle vehicle = vehicleRepository.findByDriver(driver).orElse(null);
            if (vehicle != null) {
                VehicleInfoResponse vehicleInfo = new VehicleInfoResponse();
                vehicleInfo.setId(vehicle.getId());
                vehicleInfo.setModel(vehicle.getModel());
                vehicleInfo.setLicensePlate(vehicle.getLicensePlate());
                vehicleInfo.setVehicleTypeName(vehicle.getVehicleType().getName().name());
                vehicleInfo.setNumSeats(vehicle.getNumSeats());
                vehicleInfo.setBabyFriendly(vehicle.getBabyFriendly());
                vehicleInfo.setPetFriendly(vehicle.getPetFriendly());
                response.setVehicle(vehicleInfo);
            }

            response.setActiveHoursLast24h(computeActiveHoursLast24h(driver));
        }
        
        return response;
    }
    
    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check email uniqueness if changed
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("User with email " + request.email() + " already exists");
            }
            user.setEmail(request.email());
        }
        
        // Update fields if provided
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.surname() != null) {
            user.setSurname(request.surname());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.profilePictureUrl() != null) {
            user.setProfilePictureUrl(request.profilePictureUrl());
        }
        
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        // Return updated profile
        return getProfile(userId);
    }

    /**
     * Save profile picture file to disk and return the URL path to store in DB.
     * Caller should then call updateProfile with that URL.
     */
    @Transactional
    public String saveProfilePicture(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile picture file is required");
        }
        if (file.getSize() > MAX_PICTURE_SIZE_BYTES) {
            throw new IllegalArgumentException("Profile picture must be at most 5MB");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            contentType = getContentTypeFromFilename(file.getOriginalFilename());
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Profile picture must be JPEG, PNG, GIF, or WebP");
            }
        }

        Path dir = Path.of(profileUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String extension = getExtensionFromContentTypeOrFilename(contentType, file.getOriginalFilename());
        String filename = userId + extension;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        file.transferTo(target);

        return "/api/uploads/profile/" + filename;
    }

    /**
     * Save a temporary profile picture (e.g. for registration before user exists).
     * File is stored under profile-dir/temp/ with a UUID filename.
     * Caller should store the returned URL in the user's profilePictureUrl when creating the user.
     */
    @Transactional
    public String saveTempProfilePicture(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile picture file is required");
        }
        if (file.getSize() > MAX_PICTURE_SIZE_BYTES) {
            throw new IllegalArgumentException("Profile picture must be at most 5MB");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            contentType = getContentTypeFromFilename(file.getOriginalFilename());
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Profile picture must be JPEG, PNG, GIF, or WebP");
            }
        }

        Path dir = Path.of(profileUploadDir).resolve("temp").toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String extension = getExtensionFromContentTypeOrFilename(contentType, file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        file.transferTo(target);

        return "/api/uploads/profile/temp/" + filename;
    }

    /** Strip parameters (e.g. "; charset=UTF-8") so "image/jpeg; charset=UTF-8" -> "image/jpeg". */
    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return null;
        String base = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        return base.isEmpty() ? null : base;
    }

    /** Derive content type from filename when client sends null/wrong Content-Type (e.g. photo.jpg -> image/jpeg). */
    private static String getContentTypeFromFilename(String originalFilename) {
        if (originalFilename == null) return null;
        String name = originalFilename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return null;
    }

    private static String getExtensionFromContentTypeOrFilename(String contentType, String originalFilename) {
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("png")) return ".png";
            if (lower.contains("gif")) return ".gif";
            if (lower.contains("webp")) return ".webp";
            if (lower.contains("jpeg") || lower.contains("jpg")) return ".jpg";
        }
        if (originalFilename != null) {
            String name = originalFilename.toLowerCase(Locale.ROOT);
            if (name.endsWith(".png")) return ".png";
            if (name.endsWith(".gif")) return ".gif";
            if (name.endsWith(".webp")) return ".webp";
        }
        return ".jpg";
    }

    /**
     * Computes how many hours the driver has been driving in the last 24 hours
     * (ACTIVE and FINISHED rides only), for the 8h daily limit.
     */
    private ActiveHoursResponse computeActiveHoursLast24h(Driver driver) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(24, ChronoUnit.HOURS);
        List<Ride> rides = rideRepository.findDriverRidesWithActivitySince(driver, windowStart);

        long totalSeconds = 0;
        for (Ride r : rides) {
            if (r.getStatus() != RideStatus.ACTIVE && r.getStatus() != RideStatus.FINISHED) {
                continue;
            }
            Instant start = r.getStartTime();
            if (start == null) continue;
            Instant end = r.getEndTime() != null ? r.getEndTime() : now;
            if (end.isBefore(windowStart)) continue;

            Instant effectiveStart = start.isBefore(windowStart) ? windowStart : start;
            Instant effectiveEnd = end.isAfter(now) ? now : end;
            totalSeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
        }

        double hoursWorked = totalSeconds / 3600.0;
        return new ActiveHoursResponse(hoursWorked, MAX_DRIVING_HOURS_PER_24H);
    }
}
