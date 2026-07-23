package com.example.carsharing.dto.car;

import com.example.carsharing.model.Car;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateCarRequestDto(
        @NotBlank(message = "Model can't be empty")
        String model,
        @NotBlank(message = "Brand can't be empty")
        String brand,
        @NotNull(message = "Type can't be empty")
        Car.Type type,
        @Min(value = 0, message = "Inventory cannot be negative")
        int inventory,
        @Positive(message = "Daily fee must be greater than 0")
        @NotNull
        BigDecimal dailyFee
) {
}
