package com.example.carsharing.service.impl;

import com.example.carsharing.dto.payment.CreatePaymentRequestDto;
import com.example.carsharing.dto.payment.PaymentDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.exception.EntityNotFoundException;
import com.example.carsharing.exception.PaymentProcessingException;
import com.example.carsharing.mapper.PaymentMapper;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import com.example.carsharing.notification.RentalNotificationMessage;
import com.example.carsharing.repository.payment.PaymentRepository;
import com.example.carsharing.repository.rental.RentalRepository;
import com.example.carsharing.service.NotificationService;
import com.example.carsharing.service.PaymentService;
import com.example.carsharing.service.UserService;
import com.example.carsharing.util.PaymentUtil;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {
    public static final String SESSION_COMPLETE = "checkout.session.completed";
    public static final String SESSION_EXPIRED = "checkout.session.expired";
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    private final PaymentRepository paymentRepository;
    private final PaymentUtil paymentUtil;
    private final PaymentMapper paymentMapper;
    private final RentalRepository rentalRepository;
    private final UserService userService;
    private final RentalNotificationMessage notificationMessageBuilder;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public PaymentDto createPaymentSession(CreatePaymentRequestDto requestDto) {
        Rental rental = rentalRepository.findById(requestDto.rentalId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find rental by id "
                        + requestDto.rentalId()));
        UserResponseDto loggedUser = userService.getLoggedUser();
        boolean isManager = checkRole(userService, Role.RoleName.MANAGER);
        if (!isManager && !Objects.equals(rental.getUser().getId(), loggedUser.id())) {
            throw new PaymentProcessingException("You cannot pay for someone else's rental");
        }
        if (rental.getReturnDate().isBefore(rental.getRentalDate())) {
            throw new PaymentProcessingException("Return date cannot be before rental date");
        }
        if (requestDto.type() == Payment.Type.FINE
                && rental.getActualReturnDate() == null) {
            throw new PaymentProcessingException(
                    "Cannot create fine payment before the car is returned");
        }
        if (paymentRepository.existsByRentalIdAndStatus(rental.getId(), Payment.Status.PENDING)) {
            throw new PaymentProcessingException(
                    "The pending payment already exists for rental id " + rental.getId());
        }
        BigDecimal amount = paymentUtil.calculateAmount(rental, requestDto.type());
        Session stripeSession;
        try {
            stripeSession = paymentUtil.createStripeSession(amount, rental.getId());
        } catch (StripeException e) {
            throw new PaymentProcessingException(
                    "Can't create Stripe session with rental id " + rental.getId(), e);
        }

        Payment payment = new Payment();
        payment.setStatus(Payment.Status.PENDING);
        payment.setType(requestDto.type());
        payment.setRental(rental);
        payment.setSessionUrl(stripeSession.getUrl());
        payment.setSessionId(stripeSession.getId());
        payment.setAmountToPay(amount);
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Transactional
    @Override
    public PaymentDto processSuccessfulPayment(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new PaymentProcessingException("Session ID cannot be empty");
        }
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find payment with session id " + sessionId));
        Session retrieve;
        try {
            retrieve = paymentUtil.retrieveStripeSession(sessionId);
        } catch (StripeException e) {
            throw new PaymentProcessingException(
                    "Can't retrieve Stripe session for session id: " + sessionId, e);
        }
        if (Objects.equals(retrieve.getPaymentStatus(), "paid")) {
            payment.setStatus(Payment.Status.PAID);
            paymentRepository.save(payment);
            messageSuccessPayment(payment);
        }
        return paymentMapper.toDto(payment);
    }

    @Override
    public PaymentDto processCanceledPayment(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new PaymentProcessingException("Session ID cannot be empty");
        }
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find payment with session id " + sessionId));
        if (payment.getStatus() == Payment.Status.PENDING) {
            payment.setStatus(Payment.Status.CANCELED);
            paymentRepository.save(payment);
            try {
                notificationService.send(
                        notificationMessageBuilder.buildCanceledPaymentMessage(payment));
            } catch (RestClientException e) {
                System.err.println(e.getMessage());
            }
            return paymentMapper.toDto(payment);
        }
        return paymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PaymentDto> getPayments(Long userId) {
        User loggedUser = userService.getUser();
        boolean isCustomer = checkRole(userService, Role.RoleName.CUSTOMER);
        if (isCustomer && userId != null && !Objects.equals(userId, loggedUser.getId())) {
            throw new PaymentProcessingException("Customers cannot view other users' payments");
        }
        List<Payment> payments;
        if (isCustomer) {
            payments = paymentRepository.findAllByRentalUserId(loggedUser.getId());
        } else if (userId != null) {
            payments = paymentRepository.findAllByRentalUserId(userId);
        } else {
            payments = paymentRepository.findAll();
        }
        return payments.stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    boolean checkRole(UserService userService, Role.RoleName roleName) {
        return userService.getUser().getRoles().stream()
                .anyMatch(role -> role.getRole() == roleName);
    }

    @Transactional
    @Override
    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new PaymentProcessingException("Invalid Stripe signature", e);
        }
        if (SESSION_COMPLETE.equals(event.getType())) {
            Session finalSession = getSession(event);
            if (finalSession != null && finalSession.getId() != null) {
                Payment payment = paymentRepository.findBySessionId(finalSession.getId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Can't find payment with session id " + finalSession.getId()));
                payment.setStatus(Payment.Status.PAID);
                messageSuccessPayment(payment);
                paymentRepository.save(payment);
            }
        } else if (SESSION_EXPIRED.equals(event.getType())) {
            Session session = getSession(event);
            if (session != null && session.getId() != null) {
                processCanceledPayment(session.getId());
            }
        }
    }

    private static Session getSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Session session;
        if (deserializer.getObject().isPresent()) {
            session = (Session) deserializer.getObject().get();
        } else {
            try {
                session = (Session) deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new PaymentProcessingException(
                        "Failed to deserialize Stripe checkout session", e);
            }
        }
        return session;
    }

    private void messageSuccessPayment(Payment payment) {
        try {
            notificationService.send(
                    notificationMessageBuilder.buildSuccessfulPaymentMessage(payment));
        } catch (RestClientException e) {
            System.err.println(e.getMessage());
        }
    }
}
