package com.example.carsharing.dto.car;

import jakarta.validation.constraints.NotBlank;

public record UpdateCarRequestDto(
        @NotBlank(message = "Model can't be empty")
        String model,
        @NotBlank(message = "Brand can't be empty")
        String brand
) {
}
