package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.dto.VehicleCreateRequest;
import com.ftn.drumigo.dto.VehicleLocationUpdateRequest;
import com.ftn.drumigo.dto.VehicleUpdateRequest;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.DriverRepository;
import com.ftn.drumigo.repository.VehicleRepository;
import com.ftn.drumigo.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {
    
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    
    /**
     * Get all vehicles from active drivers for display on the landing page map.
     * Returns both available (free) and busy (on ride) vehicles.
     * Only vehicles from drivers with activeDriver=true are included.
     */
    public List<Vehicle> getActiveVehicles() {
        return vehicleRepository.findByDriverActiveDriverTrue();
    }
    
    public Vehicle getById(Long id) {
        return vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }
    
    public List<Vehicle> getAll() {
        return vehicleRepository.findAll();
    }
    
    public Vehicle create(VehicleCreateRequest request) {
        Driver driver = driverRepository.findById(request.driverId())
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.driverId()));
        
        VehicleType vehicleType = vehicleTypeRepository.findById(request.vehicleTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + request.vehicleTypeId()));
        
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setVehicleType(vehicleType);
        vehicle.setNumSeats(request.numSeats());
        vehicle.setBabyFriendly(request.babyFriendly());
        vehicle.setPetFriendly(request.petFriendly());
        vehicle.setCurrentLat(request.currentLat());
        vehicle.setCurrentLng(request.currentLng());
        
        return vehicleRepository.save(vehicle);
    }
    
    public Vehicle update(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = getById(id);
        
        if (request.vehicleTypeId() != null) {
            VehicleType vehicleType = vehicleTypeRepository.findById(request.vehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + request.vehicleTypeId()));
            vehicle.setVehicleType(vehicleType);
        }
        
        if (request.numSeats() != null) {
            vehicle.setNumSeats(request.numSeats());
        }
        if (request.babyFriendly() != null) {
            vehicle.setBabyFriendly(request.babyFriendly());
        }
        if (request.petFriendly() != null) {
            vehicle.setPetFriendly(request.petFriendly());
        }
        if (request.currentLat() != null) {
            vehicle.setCurrentLat(request.currentLat());
        }
        if (request.currentLng() != null) {
            vehicle.setCurrentLng(request.currentLng());
        }
        
        return vehicleRepository.save(vehicle);
    }
    
    public Vehicle updateLocation(Long id, VehicleLocationUpdateRequest request) {
        Vehicle vehicle = getById(id);
        vehicle.setCurrentLat(request.lat());
        vehicle.setCurrentLng(request.lng());
        return vehicleRepository.save(vehicle);
    }
    
    public void delete(Long id) {
        Vehicle vehicle = getById(id);
        vehicleRepository.delete(vehicle);
    }
}

