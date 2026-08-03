package com.example.carsharing.dto.payment;

import com.example.carsharing.model.Payment;
import java.math.BigDecimal;

public record PaymentDto(
        Long id,
        Payment.Status status,
        Payment.Type type,
        Long rentalId,
        String sessionUrl,
        String sessionId,
        BigDecimal amountToPay
) {
}
