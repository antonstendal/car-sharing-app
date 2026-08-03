package com.example.carsharing.controller;

import com.example.carsharing.dto.payment.CreatePaymentRequestDto;
import com.example.carsharing.dto.payment.PaymentDto;
import com.example.carsharing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment Management", description =
        "Endpoints for managing payments and Stripe checkout sessions")
@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Create a payment session",
            description = "Creates a new Stripe checkout session for a rental or fine payment")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER')")
    @PostMapping
    public PaymentDto createPaymentSession(@RequestBody
                                           @Valid CreatePaymentRequestDto requestDto) {
        return paymentService.createPaymentSession(requestDto);
    }

    @Operation(summary = "Handle successful Stripe payment redirect",
            description = "Callback endpoint invoked by Stripe after a successful payment")
    @GetMapping("/success")
    public PaymentDto processSuccessfulPayment(@RequestParam("session_id") String sessionId) {
        return paymentService.processSuccessfulPayment(sessionId);
    }

    @Operation(summary = "Handle canceled Stripe payment redirect",
            description = "Callback endpoint invoked by Stripe"
                    + " when user cancels the checkout process")
    @GetMapping("/cancel")
    public PaymentDto processCanceledPayment(@RequestParam("session_id") String sessionId) {
        return paymentService.processCanceledPayment(sessionId);
    }

    @Operation(summary = "Get user payments",
            description = "Retrieves payments. Customers see only their payments,"
                    + " managers can filter by user_id or retrieve all payments")
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER')")
    @GetMapping
    public List<PaymentDto> getPayments(
            @RequestParam(required = false, name = "user_id") Long userId) {
        return paymentService.getPayments(userId);
    }

    @Operation(summary = "Stripe webhook endpoint",
            description = "Handles incoming Stripe events asynchronously")
    @PostMapping("/webhook")
    public void handleWebhook(@RequestBody String payload,
                              @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhookEvent(payload, sigHeader);
    }
}
