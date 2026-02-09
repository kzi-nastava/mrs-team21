package com.drumigo.mobile.data.remote.dto.auth.request;

public class PassengerRegisterRequest {

    public final String email;
    public final String password;
    public final String confirmPassword;
    public final String firstName;
    public final String lastName;
    public final String address;
    public final String phoneNumber;
    public final String profilePicture;

    public PassengerRegisterRequest(
            String email,
            String password,
            String confirmPassword,
            String firstName,
            String lastName,
            String address,
            String phoneNumber,
            String profilePicture
    ) {
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.profilePicture = profilePicture;
    }
}
