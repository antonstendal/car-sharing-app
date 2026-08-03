package com.example.carsharing.repository.payment;

import com.example.carsharing.model.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"rental"})
    Optional<Payment> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = {"rental"})
    List<Payment> findAllByRentalUserId(Long userId);

    @EntityGraph(attributePaths = {"rental"})
    List<Payment> findAll();

    boolean existsByRentalIdAndStatus(Long rentalId, Payment.Status status);
}
