package com.ftn.drumigo.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Password must be 6-64 chars, contain at least one uppercase letter, one lowercase letter, and one digit or special character
    public boolean isValid(String password) {
        if (password == null) return false;
        return password.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9\\W]).{6,64}");
    }
}
