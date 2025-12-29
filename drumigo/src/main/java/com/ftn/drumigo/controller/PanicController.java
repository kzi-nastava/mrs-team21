package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.PanicEvent;
import com.ftn.drumigo.dto.PanicCreateRequest;
import com.ftn.drumigo.dto.PanicEventResponse;
import com.ftn.drumigo.mapper.PanicEventMapper;
import com.ftn.drumigo.service.PanicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PanicController {
    
    private final PanicService panicService;
    private final PanicEventMapper panicEventMapper;
    
    @PostMapping("/rides/{rideId}/panic")
    public ResponseEntity<PanicEventResponse> createPanic(
            @PathVariable Long rideId,
            @RequestParam Long userId,
            @Valid @RequestBody PanicCreateRequest request) {
        PanicEvent panicEvent = panicService.create(rideId, userId, request);
        return ResponseEntity.status(201).body(panicEventMapper.toResponse(panicEvent));
    }
    
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

