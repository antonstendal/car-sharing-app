package com.example.carsharing.controller;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.car.CreateCarRequestDto;
import com.example.carsharing.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Car managing", description = "Endpoints for handling cars")
@RequiredArgsConstructor
@RestController
@RequestMapping("/cars")
public class CarController {
    private final CarService carService;

    @Operation(summary = "Create a car", description = "Create a new car")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CarDto create(@RequestBody @Valid CreateCarRequestDto requestDto) {
        return carService.save(requestDto);
    }

    @Operation(summary = "Get all cars", description = "Return a paginated list of cars")
    @GetMapping
    public Page<CarDto> findAll(@ParameterObject Pageable pageable) {
        return carService.findAll(pageable);
    }

    @Operation(summary = "Get car by id", description = "Return car by id")
    @GetMapping("/{id}")
    public CarDto findById(@PathVariable Long id) {
        return carService.findById(id);
    }

    @Operation(summary = "Update a car", description = "Update an existent car identified by id")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public CarDto update(@PathVariable Long id,
                         @RequestBody @Valid CreateCarRequestDto requestDto) {
        return carService.update(id, requestDto);
    }

    @Operation(summary = "Delete a car", description = "Delete a car identified by id")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        carService.deleteById(id);
    }
}
