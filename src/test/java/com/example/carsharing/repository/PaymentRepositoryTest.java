package com.example.carsharing.repository;

import com.example.carsharing.model.Payment;
import com.example.carsharing.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@ImportAutoConfiguration(classes = LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {"classpath:database/payment/clean/remove-all-payments.sql",
        "classpath:database/payment/add-payments.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Should return payment when sessionId matches")
    void findBySessionId_paymentExists_returnsPayment() {
        Optional<Payment> actual = paymentRepository.findBySessionId("session-abc");

        assertTrue(actual.isPresent());
        assertEquals("https://session/abc",
                actual.get().getSessionUrl());
        assertEquals(0, new BigDecimal("100.00").compareTo(
                actual.get().getAmountToPay()));
        assertEquals(1L, actual.get().getRental().getId());
        assertEquals(Payment.Type.PAYMENT, actual.get().getType());
        assertEquals(Payment.Status.PENDING, actual.get().getStatus());
    }

    @Test
    @DisplayName("Should return empty Optional when sessionId does not match any payment")
    void findBySessionId_paymentDoesNotExist_returnsEmpty() {
        Optional<Payment> actual = paymentRepository.findBySessionId("session-123456234234");
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Should return payments for given userId")
    void findAllByRentalUserId_userHasPayments_returnsPaymentsList() {
        List<Payment> actual = paymentRepository.findAllByRentalUserId(2L);
        assertEquals(1, actual.size());
        assertEquals("https://session/xyz", actual.get(0).getSessionUrl());
        assertEquals("session-xyz", actual.get(0).getSessionId());
        assertEquals(0, new BigDecimal("200.00").compareTo(
                actual.get(0).getAmountToPay()));
    }

    @Test
    @DisplayName("Should return empty list when user has no payments")
    void findAllByRentalUserId_userHasNoPayments_returnsEmptyList() {
        List<Payment> actual = paymentRepository.findAllByRentalUserId(999L);
        assertTrue(actual.isEmpty());
    }
}
