package com.example.carsharing.service.impl;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.car.CreateCarRequestDto;
import com.example.carsharing.dto.car.UpdateCarRequestDto;
import com.example.carsharing.exception.EntityAlreadyExistsException;
import com.example.carsharing.exception.EntityNotFoundException;
import com.example.carsharing.exception.RentalProcessingException;
import com.example.carsharing.mapper.CarMapper;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.car.CarRepository;
import com.example.carsharing.repository.rental.RentalRepository;
import com.example.carsharing.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final RentalRepository rentalRepository;

    @Override
    public CarDto save(CreateCarRequestDto requestDto) {
        String model = requestDto.model();
        String brand = requestDto.brand();
        Car.Type type = requestDto.type();
        if (carRepository.findByModelAndBrandAndType(model, brand, type).isPresent()) {
            throw new EntityAlreadyExistsException(
                    "The car with model " + model
                            + " , brand " + brand
                            + " , type " + type
                            + " already exists");
        }
        Car newCar = carMapper.toModel(requestDto);
        return carMapper.toDto(carRepository.save(newCar));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CarDto> findAll(Pageable pageable) {
        return carRepository
                .findAll(pageable)
                .map(carMapper::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public CarDto findById(Long id) {
        Car car = carRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find car by id " + id));
        return carMapper.toDto(car);
    }

    @Override
    public CarDto update(Long id, UpdateCarRequestDto requestDto) {
        Car car = carRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find car by id " + id));
        carMapper.update(requestDto, car);

        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    public void deleteById(Long id) {
        Car car = carRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find car by id " + id));
        if (rentalRepository.existsByCarIdAndActualReturnDateIsNull(id)) {
            throw new RentalProcessingException("Cannot delete car with id " + id
                    + " because it has active rentals");
        }
        carRepository.delete(car);
    }
}
