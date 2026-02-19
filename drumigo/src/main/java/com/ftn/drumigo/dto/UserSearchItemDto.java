package com.ftn.drumigo.dto;

/**
 * Minimal user info for admin search (e.g. report "one person" picker).
 */
public record UserSearchItemDto(Long id, String email, String name, String surname) {}
