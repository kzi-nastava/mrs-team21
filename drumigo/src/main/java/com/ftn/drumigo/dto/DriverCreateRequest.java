package com.ftn.drumigo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DriverCreateRequest(
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "Surname is required")
    String surname,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    String address,
    String phone,
    
    String licenseNumber,
    
    // Vehicle fields
    @NotNull(message = "Vehicle type ID is required")
    @Positive(message = "Vehicle type ID must be positive")
    Long vehicleTypeId,
    
    @NotBlank(message = "Vehicle model is required")
    String vehicleModel,
    
    @NotBlank(message = "License plate is required")
    String vehicleLicensePlate,
    
    @NotNull(message = "Number of seats is required")
    @Positive(message = "Number of seats must be positive")
    Integer vehicleNumSeats,
    
    Boolean vehicleBabyFriendly,
    Boolean vehiclePetFriendly
) {}

