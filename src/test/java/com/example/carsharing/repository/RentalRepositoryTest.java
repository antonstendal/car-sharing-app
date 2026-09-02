package com.example.carsharing.repository;

import com.example.carsharing.model.Rental;
import com.example.carsharing.repository.rental.RentalRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@ImportAutoConfiguration(classes = LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {"classpath:database/rental/clean/remove-all-rentals.sql",
        "classpath:database/rental/add-rentals.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RentalRepositoryTest {
    @Autowired
    private RentalRepository rentalRepository;

    @Test
    @DisplayName("Should return rentals for given userId")
    void searchRentals_userIdProvided_returnsUserRentals() {
        Page<Rental> actual = rentalRepository.searchRentals(
                1L,
                true,
                PageRequest.of(0, 10));
        assertEquals(1, actual.getTotalElements());
        assertTrue(actual.stream().anyMatch(rental -> rental.getId().equals(1L)));
    }

    @Test
    @DisplayName("Should return empty page when userId does not match any rentals")
    void searchRentals_userIdDoesNotMatch_returnsEmptyPage() {
        Page<Rental> actual = rentalRepository.searchRentals(
                999L,
                false,
                PageRequest.of(0, 10));
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Should return only active rentals when isActive = true")
    void searchRentals_isActiveTrue_returnsActiveRentals() {
        Page<Rental> actual = rentalRepository.searchRentals(
                null,
                true,
                PageRequest.of(0, 10));
        assertEquals(2, actual.getTotalElements());
        assertTrue(actual.stream().anyMatch(r -> r.getId().equals(1L)));
    }

    @Test
    @DisplayName("Should return overdue active rentals")
    void findByReturnDateLessThanEqualAndActualReturnDateIsNull_overdueExists_returnsList() {
        List<Rental> actual = rentalRepository
                .findByReturnDateLessThanEqualAndActualReturnDateIsNull(
                        LocalDate.of(2024, 1, 5));
        assertEquals(1, actual.size());
        assertEquals(1L, actual.get(0).getId());
    }

    @Test
    @DisplayName("Should return empty list when no overdue rentals exist")
    void findByReturnDateLessThanEqualAndActualReturnDateIsNull_noOverdue_returnsEmptyList() {
        List<Rental> actual = rentalRepository
                .findByReturnDateLessThanEqualAndActualReturnDateIsNull(
                        LocalDate.of(2023, 12, 25));
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Should return true when active rental exists for car")
    void existsByCarIdAndActualReturnDateIsNull_activeRentalExists_returnsTrue() {
        boolean actual = rentalRepository.existsByCarIdAndActualReturnDateIsNull(1L);
        assertTrue(actual);
    }

    @Test
    @DisplayName("Should return false when no active rental exists for car")
    void existsByCarIdAndActualReturnDateIsNull_noActiveRental_returnsFalse() {
        boolean actual = rentalRepository.existsByCarIdAndActualReturnDateIsNull(999L);
        assertFalse(actual);
    }
}
