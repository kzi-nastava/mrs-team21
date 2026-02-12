package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.DriverProfileChangeRequest;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.*;
import com.ftn.drumigo.dto.history.request.RideHistoryRequest;
import com.ftn.drumigo.dto.ride.response.RideResponse;
import com.ftn.drumigo.mapper.DriverProfileChangeRequestMapper;
import com.ftn.drumigo.mapper.RideMapper;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final RideService rideService;
    private final RideMapper rideMapper;
    private final DriverProfileChangeRequestService profileChangeRequestService;
    private final DriverProfileChangeRequestMapper profileChangeRequestMapper;
    private final ReportService reportService;
    
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
    public ResponseEntity<ReportResponse> getAdminReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        ReportResponse report = reportService.getAdminReport(from, to);
        return ResponseEntity.ok(report);
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

