package com.ftn.drumigo.controller;

import com.ftn.drumigo.service.DatabaseMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final DatabaseMaintenanceService maintenanceService;

    @PostMapping("/reset")
    public ResponseEntity<Void> resetDatabase() {
        maintenanceService.resetDatabase();
        return ResponseEntity.ok().build();
    }
}
