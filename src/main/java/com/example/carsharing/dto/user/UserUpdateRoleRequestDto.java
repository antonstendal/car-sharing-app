package com.example.carsharing.dto.user;

import com.example.carsharing.model.Role;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRoleRequestDto(
        @NotNull(message = "User role can't be null")
        Role.RoleName roleName
) {
}
