package com.example.carsharing.notification;

import com.example.carsharing.dto.rental.RentalDto;

public interface RentalNotificationMessage {
    String buildNewRentalMessage(RentalDto rental);

    String buildReturnRentalMessage(RentalDto rental);

    String buildOverdueRentalMessage(RentalDto rental);

    String buildNoRentalsOverdueMessage();
}
