package com.example.carsharing.dto.rental;

import com.example.carsharing.dto.car.CarDto;
import java.time.LocalDate;

public record RentalDto(
        Long id,
        LocalDate rentalDate,
        LocalDate returnDate,
        LocalDate actualReturnDate,
        CarDto car,
        Long userId
) {
}
