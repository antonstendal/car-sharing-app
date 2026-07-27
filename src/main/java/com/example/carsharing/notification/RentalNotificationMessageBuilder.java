package com.example.carsharing.notification;

import com.example.carsharing.dto.rental.RentalDto;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class RentalNotificationMessageBuilder implements RentalNotificationMessage {
    @Override
    public String buildNewRentalMessage(RentalDto rental) {
        return """
                🚗 New Car Rental Created!
                
                🆔 Rental ID: %d
                🚘 Car: %s %s %s
                📅 Rental Date: %s
                📆 Expected Return Date: %s
                💰 Daily Fee: $%s
                """
                .formatted(rental.id(),
                        rental.car().brand(),
                        rental.car().model(),
                        rental.car().type().toString().toLowerCase(),
                        rental.rentalDate(),
                        rental.returnDate(),
                        rental.car().dailyFee()
                );
    }

    @Override
    public String buildReturnRentalMessage(RentalDto rental) {
        return """
                Car Return Confirmation
                
                🆔 Rental ID: %d
                🚘 Car: %s %s
                🏁 Actual Return Date: %s
                """.formatted(
                rental.id(),
                rental.car().brand(),
                rental.car().model(),
                rental.actualReturnDate()
        );
    }

    @Override
    public String buildOverdueRentalMessage(RentalDto rental) {
        return """
                Dear Customer, your rental period has expired.
                
                🆔 Rental ID: %d
                🚘 Car: %s %s
                📆 Expected Return Date: %s
                ⏱️ Days Overdue: %d
                """.formatted(
                rental.id(),
                rental.car().brand(),
                rental.car().model(),
                rental.returnDate(),
                ChronoUnit.DAYS.between(rental.returnDate(), LocalDate.now())
        );
    }

    @Override
    public String buildNoRentalsOverdueMessage() {
        return "No rentals overdue today!";
    }
}
