package com.example.carsharing.repository.rental;

import com.example.carsharing.model.Rental;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    @Query("""
            SELECT r FROM Rental r
            WHERE (:userId is NULL OR r.user.id = :userId)
              AND (:isActive IS NULL OR
                  (:isActive = true AND r.actualReturnDate IS NULL) OR
                  (:isActive = false AND r.actualReturnDate IS NOT NULL))
            """)
    Page<Rental> searchRentals(
            @Param("userId") Long userId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    List<Rental> findByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate date);

    boolean existsByCarIdAndActualReturnDateIsNull(Long carId);
}
