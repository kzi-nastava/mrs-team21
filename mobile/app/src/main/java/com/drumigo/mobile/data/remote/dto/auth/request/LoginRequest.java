package com.drumigo.mobile.data.remote.dto.auth.request;

public class LoginRequest {

    public final String email;
    public final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
