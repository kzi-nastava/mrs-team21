package com.drumigo.mobile.data.model.user;

/**
 * Minimal user info for admin search (e.g. notifications user picker).
 * Matches backend UserSearchItemDto.
 */
public class UserSearchItem {
    public Long id;
    public String email;
    public String name;
    public String surname;
}
