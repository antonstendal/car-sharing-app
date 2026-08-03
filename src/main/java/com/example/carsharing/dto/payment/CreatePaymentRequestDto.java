package com.example.carsharing.dto.payment;

import com.example.carsharing.model.Payment;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequestDto(
        @NotNull(message = "Rental ID cannot be null")
        Long rentalId,

        @NotNull(message = "Payment type cannot be null")
        Payment.Type type
) {
}
