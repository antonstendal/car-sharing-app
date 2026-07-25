package com.example.carsharing.service;

import com.example.carsharing.dto.rental.CreateRentalRequestDto;
import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.exception.CarOutOfStockException;
import com.example.carsharing.exception.RentalAlreadyReturnedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalService {
    Page<RentalDto> getRentals(Long userId, Boolean isActive, Pageable pageable);

    RentalDto getRentalById(Long id);

    RentalDto createRental(CreateRentalRequestDto requestDto) throws CarOutOfStockException;

    RentalDto returnRental(Long id) throws RentalAlreadyReturnedException;
}
