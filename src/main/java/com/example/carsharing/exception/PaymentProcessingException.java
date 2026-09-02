package com.example.carsharing.exception;

import com.stripe.exception.StripeException;

public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message, StripeException e) {
        super(message);
    }

    public PaymentProcessingException(String message) {
        super(message);
    }
}
