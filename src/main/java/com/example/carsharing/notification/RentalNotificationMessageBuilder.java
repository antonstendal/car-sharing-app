package com.example.carsharing.notification;

import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.model.Payment;
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
                💰 Daily Fee: PLN %s
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

    @Override
    public String buildSuccessfulPaymentMessage(Payment payment) {
        Long rentalId = (payment.getRental() != null) ? payment.getRental().getId() : null;
        return """
            Payment Successful!
            
            🆔 Payment ID: %d
            💳 Session ID: %s
            🚗 Rental ID: %s
            💰 Amount Paid: PLN %s
            Status: PAID
            """.formatted(
                payment.getId(),
                payment.getSessionId(),
                rentalId != null ? rentalId.toString() : "N/A",
                payment.getAmountToPay()
        );
    }

    @Override
    public String buildCanceledPaymentMessage(Payment payment) {
        Long rentalId = (payment.getRental() != null) ? payment.getRental().getId() : null;
        return """
            Payment Canceled or Expired
            
            🆔 Payment ID: %d
            🚗 Rental ID: %s
            💰 Amount: PLN %s
            Status: CANCELED
            """.formatted(
                payment.getId(),
                rentalId != null ? rentalId.toString() : "N/A",
                payment.getAmountToPay()
        );
    }
}
