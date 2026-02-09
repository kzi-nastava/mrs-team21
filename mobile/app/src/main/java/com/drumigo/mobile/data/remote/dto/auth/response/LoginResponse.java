package com.drumigo.mobile.data.remote.dto.auth.response;

public class LoginResponse {

    public Long userId;
    public String email;
    public String role;
    public String token;

    public LoginResponse() {
    }

    public LoginResponse(Long userId, String email, String role, String token) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.token = token;
    }
}
