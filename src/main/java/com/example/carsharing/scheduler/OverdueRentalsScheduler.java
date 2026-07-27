package com.example.carsharing.scheduler;

import com.example.carsharing.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OverdueRentalsScheduler {
    private final RentalService rentalService;

    @Scheduled(cron = "0 0 9 * * *")
    public void checkOverdueRentals() {
        rentalService.notifyOverdueRentals();
    }
}
