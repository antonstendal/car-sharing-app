package com.example.carsharing.controller;

import com.example.carsharing.dto.rental.CreateRentalRequestDto;
import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.exception.CarOutOfStockException;
import com.example.carsharing.exception.RentalAlreadyReturnedException;
import com.example.carsharing.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Tag(name = "Rental managing", description = "Endpoints for handling rentals")
@RestController
@RequestMapping("/rentals")
public class RentalController {
    private final RentalService rentalService;

    @Operation(summary = "Create a new rental",
            description = "Add a new car rental for the current user "
                    + "and decrease car inventory by 1")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalDto create(@RequestBody @Valid CreateRentalRequestDto requestDto)
            throws CarOutOfStockException {
        return rentalService.createRental(requestDto);
    }

    @Operation(summary = "Get all rentals",
            description = "Retrieve a paginated list of rentals"
                    + " filtered by user ID and active status")
    @GetMapping
    public Page<RentalDto> getAll(@RequestParam(name = "user_Id", required = false) Long userId,
                                  @RequestParam(name = "is_active", required = false)
                                  Boolean isActive,
                                  @PageableDefault(sort = "id") Pageable pageable) {
        return rentalService.getRentals(userId, isActive, pageable);
    }

    @Operation(summary = "Get rental by id", description = "Get specific rental details by its ID")
    @GetMapping("/{id}")
    public RentalDto getById(@PathVariable Long id) {
        return rentalService.getRentalById(id);
    }

    @Operation(summary = "Create return rental",
            description = "Set the actual return date for a rental")
    @PostMapping("/{id}/return")
    public RentalDto rentalReturn(@PathVariable Long id) throws RentalAlreadyReturnedException {
        return rentalService.returnRental(id);
    }
}
