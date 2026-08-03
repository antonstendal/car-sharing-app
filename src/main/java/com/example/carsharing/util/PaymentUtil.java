package com.example.carsharing.util;

import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class PaymentUtil {
    public static final String CANCEL_PAYMENT_URL = "http://localhost:8080/payments/cancel";
    public static final String SUCCESS_PAYMENT_URL =
            "http://localhost:8080/payments/success?session_id={CHECKOUT_SESSION_ID}";
    private static final BigDecimal FINE_MULTIPLIER = BigDecimal.valueOf(1.5);

    public Session createStripeSession(BigDecimal amount, Long rentalId)
            throws StripeException {

        long convertAmount = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(
                        SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(SUCCESS_PAYMENT_URL)
                .setCancelUrl(CANCEL_PAYMENT_URL)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("pln")
                                                .setUnitAmount(convertAmount)
                                                .setProductData(
                                                        SessionCreateParams
                                                                .LineItem
                                                                .PriceData
                                                                .ProductData.builder()
                                                                .setName("Car Rental Payment - ID: "
                                                                        + rentalId)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();
        return Session.create(params);
    }

    public BigDecimal calculateAmount(Rental rental, Payment.Type type) {
        BigDecimal dailyFee = rental.getCar().getDailyFee();

        if (type == Payment.Type.PAYMENT) {
            long days = ChronoUnit.DAYS.between(
                    rental.getRentalDate(),
                    rental.getReturnDate());
            long actualDays = Math.max(1, days);

            return dailyFee.multiply(BigDecimal.valueOf(actualDays));
        } else {
            if (rental.getActualReturnDate() == null) {
                throw new IllegalStateException("Can't calculate fine."
                        + " The car has not been returned yet.");
            }
            long overDays = ChronoUnit.DAYS.between(
                    rental.getReturnDate(),
                    rental.getActualReturnDate());
            if (overDays <= 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(overDays)
                    .multiply(dailyFee)
                    .multiply(FINE_MULTIPLIER);
        }
    }
}
