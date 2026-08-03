package com.example.carsharing.service;

import com.example.carsharing.dto.payment.CreatePaymentRequestDto;
import com.example.carsharing.dto.payment.PaymentDto;
import java.util.List;

public interface PaymentService {
    PaymentDto createPaymentSession(CreatePaymentRequestDto requestDto);

    PaymentDto processSuccessfulPayment(String sessionId);

    PaymentDto processCanceledPayment(String sessionId);

    List<PaymentDto> getPayments(Long userId);

    void handleWebhookEvent(String payload, String sigHeader);
}
