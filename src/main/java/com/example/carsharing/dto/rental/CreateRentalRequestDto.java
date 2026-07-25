package com.example.carsharing.dto.rental;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRentalRequestDto(
        @NotNull
        Long carId,
        @NotNull
        @FutureOrPresent
        LocalDate returnDate
) {
}
