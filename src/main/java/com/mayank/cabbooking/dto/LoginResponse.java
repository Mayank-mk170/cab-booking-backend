
package com.mayank.cabbooking.dto;

public class LoginResponse {

    private String token;
    private AuthResponse user;

    public LoginResponse(String token, AuthResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public AuthResponse getUser() {
        return user;
    }
}
