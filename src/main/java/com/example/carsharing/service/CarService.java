package com.example.carsharing.service;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.car.CreateCarRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {
    CarDto save(CreateCarRequestDto requestDto);

    Page<CarDto> findAll(Pageable pageable);

    CarDto findById(Long id);

    CarDto update(Long id, CreateCarRequestDto requestDto);

    void deleteById(Long id);
}
