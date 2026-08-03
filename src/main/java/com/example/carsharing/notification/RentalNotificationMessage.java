package com.example.carsharing.notification;

import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.model.Payment;

public interface RentalNotificationMessage {
    String buildNewRentalMessage(RentalDto rental);

    String buildReturnRentalMessage(RentalDto rental);

    String buildOverdueRentalMessage(RentalDto rental);

    String buildNoRentalsOverdueMessage();

    String buildSuccessfulPaymentMessage(Payment payment);

    String buildCanceledPaymentMessage(Payment payment);
}
