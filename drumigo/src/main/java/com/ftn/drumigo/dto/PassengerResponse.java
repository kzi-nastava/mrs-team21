package com.ftn.drumigo.dto;

public record PassengerResponse(
    Long id,
    String name,
    String surname,
    String email
) {}

