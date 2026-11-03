package com.evidencechain.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "must contain only letters, numbers, dot, underscore, or hyphen")
        String username,

        @NotBlank
        @Size(min = 12, message = "must be at least 12 characters long")
        String password,

        @NotNull Role role) {
}
