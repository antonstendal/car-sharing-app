package com.example.carsharing.dto.user;

import java.util.Set;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}
