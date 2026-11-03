package com.evidencechain.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {

    public static AuthResponse from(JwtService.IssuedToken token) {
        return new AuthResponse(token.accessToken(), "Bearer", token.expiresIn());
    }
}
