package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token refresh response payload containing newly issued tokens")
public class RefreshTokenResponse {

    @Schema(description = "Newly issued JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Newly rotated refresh token", example = "b8c4d3e2-f6a7-5b9c-0d1e-2f3a4b5c6d7e")
    private String refreshToken;

    public RefreshTokenResponse() {
    }

    public RefreshTokenResponse(String token) {
        this.token = token;
    }

    public RefreshTokenResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
