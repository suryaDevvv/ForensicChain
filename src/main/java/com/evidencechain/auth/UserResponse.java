package com.evidencechain.auth;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
