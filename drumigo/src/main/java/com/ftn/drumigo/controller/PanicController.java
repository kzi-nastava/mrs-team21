package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.PanicEvent;
import com.ftn.drumigo.dto.PanicEventResponse;
import com.ftn.drumigo.mapper.PanicEventMapper;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.PanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PanicController {

    private final PanicService panicService;
    private final PanicEventMapper panicEventMapper;

    @PostMapping("/rides/{rideId}/panic")
    public ResponseEntity<Void> createPanic(
            @PathVariable Long rideId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        panicService.create(rideId, userDetails.getUserId());
        return ResponseEntity.status(201).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/panic-events")
    public ResponseEntity<Page<PanicEventResponse>> getAllPanicEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<PanicEvent> panicEvents = panicService.getAll(pageable);
        Page<PanicEventResponse> responses = panicEvents.map(panicEventMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }
}
