package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.dto.EstimateRequest;
import com.ftn.drumigo.dto.EstimateResponse;
import com.ftn.drumigo.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnregisteredService {
    
    private final VehicleTypeRepository vehicleTypeRepository;
    
    public EstimateResponse getEstimate(EstimateRequest request) {
        // Calculate distance using Haversine formula
        BigDecimal distance = calculateDistance(
            request.startLat(), request.startLng(),
            request.destinationLat(), request.destinationLng()
        );
        
        // Estimate duration (stub: assume 50 km/h average speed)
        int estimatedSeconds = 0;
        if (distance.compareTo(BigDecimal.ZERO) > 0) {
            estimatedSeconds = distance.divide(new BigDecimal("50"), 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("3600"))
                .intValue();
        }
        int estimatedMinutes = estimatedSeconds / 60;
        
        // Get all vehicle types and calculate costs
        List<VehicleType> vehicleTypes = vehicleTypeRepository.findAll();
        Map<String, BigDecimal> costByType = new HashMap<>();
        
        for (VehicleType vehicleType : vehicleTypes) {
            BigDecimal cost = vehicleType.getStartPrice()
                .add(distance.multiply(vehicleType.getPricePerKm()));
            costByType.put(vehicleType.getName().name(), cost);
        }
        
        // Placeholder polyline (in production, use routing service)
        String routePolyline = "placeholder_polyline";
        
        return new EstimateResponse(
            estimatedMinutes,
            distance,
            costByType,
            routePolyline
        );
    }
    
    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        // Haversine formula
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1Rad) * Math.cos(lat2Rad)
            * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        final int EARTH_RADIUS_KM = 6371;
        double distance = EARTH_RADIUS_KM * c;
        
        return BigDecimal.valueOf(distance).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

