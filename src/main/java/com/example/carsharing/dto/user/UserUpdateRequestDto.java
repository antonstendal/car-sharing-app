package com.example.carsharing.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequestDto(
        @NotBlank(message = "First name cannot be blank")
        String firstName,
        @NotBlank(message = "Last name cannot be blank")
        String lastName
) {
}
