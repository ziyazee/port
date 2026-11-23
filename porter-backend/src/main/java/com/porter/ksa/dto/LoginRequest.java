package com.porter.ksa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 80, message = "Username must be 80 characters or fewer")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
