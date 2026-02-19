package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.DriverProfileChangeRequest;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.*;
import com.ftn.drumigo.dto.history.request.RideHistoryRequest;
import com.ftn.drumigo.dto.ReportChartResponse;
import com.ftn.drumigo.dto.ride.response.RideResponse;
import com.ftn.drumigo.dto.UserSearchItemDto;
import com.ftn.drumigo.mapper.DriverProfileChangeRequestMapper;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.mapper.UserMapper;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.DriverProfileChangeRequestService;
import com.ftn.drumigo.service.ReportService;
import com.ftn.drumigo.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

import static com.ftn.drumigo.domain.enums.UserRole.DRIVER;
import static com.ftn.drumigo.domain.enums.UserRole.PASSENGER;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RideService rideService;
    private final RideMapper rideMapper;
    private final DriverProfileChangeRequestService profileChangeRequestService;
    private final DriverProfileChangeRequestMapper profileChangeRequestMapper;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * List users (PASSENGER and DRIVER only) for admin user management. Paginated.
     */
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "email,asc") String sort,
            @RequestParam(required = false) UserRole role) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        List<UserRole> roles = role != null ? List.of(role) : List.of(PASSENGER, DRIVER);
        Page<User> users = userRepository.findByRoleIn(roles, pageable);
        Page<UserResponse> mapped = users.map(userMapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(mapped));
    }
    
    @GetMapping("/profile-change-requests")
    public ResponseEntity<Page<DriverProfileChangeRequestResponse>> getProfileChangeRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<DriverProfileChangeRequest> requests = profileChangeRequestService.getPendingRequests(pageable);
        Page<DriverProfileChangeRequestResponse> responses = requests.map(profileChangeRequestMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/profile-change-requests/{id}/approve")
    public ResponseEntity<DriverProfileChangeRequestResponse> approveProfileChangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        DriverProfileChangeRequest request = profileChangeRequestService.approve(id, adminDetails.getUserId());
        return ResponseEntity.ok(profileChangeRequestMapper.toResponse(request));
    }
    
    @PutMapping("/profile-change-requests/{id}/reject")
    public ResponseEntity<DriverProfileChangeRequestResponse> rejectProfileChangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        DriverProfileChangeRequest request = profileChangeRequestService.reject(id, adminDetails.getUserId());
        return ResponseEntity.ok(profileChangeRequestMapper.toResponse(request));
    }
    
    @GetMapping("/reports")
    public ResponseEntity<?> getAdminReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long userId) {
        if (scope != null && !scope.isBlank()) {
            ReportChartResponse chart = reportService.getChartReportForAdmin(from, to, scope.trim(), userId);
            return ResponseEntity.ok(chart);
        }
        ReportResponse report = reportService.getAdminReport(from, to);
        return ResponseEntity.ok(report);
    }

    /**
     * Search users by email (for report "one person" picker). Returns at most 15 suggestions.
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<UserSearchItemDto>> searchUsersByEmail(
            @RequestParam String q,
            @RequestParam(defaultValue = "15") int limit) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        int size = Math.min(Math.max(1, limit), 50);
        Pageable pageable = PageRequest.of(0, size, Sort.by("email").ascending());
        return ResponseEntity.ok(
                userRepository.findByEmailContainingIgnoreCase(q.trim(), pageable)
                        .stream()
                        .map(u -> new UserSearchItemDto(u.getId(), u.getEmail(), u.getName(), u.getSurname()))
                        .toList());
    }

    @GetMapping("/rides/history")
    public ResponseEntity<Page<RideResponse>> getAdminRideHistory(
            @Valid @ModelAttribute RideHistoryRequest request) {

        Pageable pageable = request.toPageable();
        List<RideStatus> statuses = request.parseStatuses();

        Page<Ride> rides = rideService.getAdminRideHistory(request.getFrom(), request.getTo(), statuses, request.getHasPanic(), pageable);
        Page<RideResponse> responses = rides.map(ride -> {
            List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
            return rideMapper.toResponse(ride, waypoints);
        });

        return ResponseEntity.ok(responses);
    }
}

