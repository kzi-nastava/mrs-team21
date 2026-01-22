package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.dto.VehicleTypeUpdateRequest;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleTypeService {
    
    private final VehicleTypeRepository vehicleTypeRepository;
    
    public List<VehicleType> getAll() {
        return vehicleTypeRepository.findAll();
    }
    
    public VehicleType getById(Long id) {
        return vehicleTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + id));
    }
    
    public VehicleType update(Long id, VehicleTypeUpdateRequest request) {
        VehicleType vehicleType = getById(id);
        vehicleType.setStartPrice(request.startPrice());
        vehicleType.setPricePerKm(request.pricePerKm());
        return vehicleTypeRepository.save(vehicleType);
    }
}

