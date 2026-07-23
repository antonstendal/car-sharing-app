package com.example.carsharing.dto.user;

import com.example.carsharing.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@FieldMatch(
        field = "password",
        fieldMatch = "repeatPassword"
)
public record UserRegistrationRequestDto(
        @Email
        @NotBlank
        String email,
        @NotBlank
        @Size(min = 8)
        String password,
        @NotBlank
        String repeatPassword,
        @NotBlank
        String firstName,
        String lastName
) {
}
