package com.example.carsharing.dto.car;

import com.example.carsharing.model.Car;
import java.math.BigDecimal;

public record CarDto(
        Long id,
        String model,
        String brand,
        Car.Type type,
        int inventory,
        BigDecimal dailyFee
) {
}
