package com.example.carsharing.service.impl;

import com.example.carsharing.dto.rental.CreateRentalRequestDto;
import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.exception.CarOutOfStockException;
import com.example.carsharing.exception.EntityNotFoundException;
import com.example.carsharing.exception.RentalAlreadyReturnedException;
import com.example.carsharing.mapper.RentalMapper;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import com.example.carsharing.notification.RentalNotificationMessage;
import com.example.carsharing.repository.car.CarRepository;
import com.example.carsharing.repository.rental.RentalRepository;
import com.example.carsharing.service.NotificationService;
import com.example.carsharing.service.RentalService;
import com.example.carsharing.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
@Service
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final UserService userService;
    private final CarRepository carRepository;
    private final NotificationService notificationService;
    private final RentalNotificationMessage notificationMessageBuilder;

    @Override
    public Page<RentalDto> getRentals(Long userId, Boolean isActive, Pageable pageable) {
        User loggedUser = userService.getUser();
        Long targetUserId = checkRoleIsManager(loggedUser) ? userId : loggedUser.getId();
        return rentalRepository.searchRentals(targetUserId, isActive, pageable)
                .map(rentalMapper::toDto);

    }

    @Override
    public RentalDto getRentalById(Long id) {
        Rental rental = rentalRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find rental with id " + id));
        User loggedUser = userService.getUser();
        if (!checkRoleIsManager(loggedUser) && !Objects.equals(rental.getUser().getId(),
                loggedUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view this rental");
        }
        return rentalMapper.toDto(rental);
    }

    @Transactional
    @Override
    public RentalDto createRental(CreateRentalRequestDto requestDto)
            throws CarOutOfStockException {
        Car car = carRepository.findById(requestDto.carId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find car by id " + requestDto.carId()));
        if (car.getInventory() == 0) {
            throw new CarOutOfStockException("Car with id " + car.getId()
                    + " currently is not available");
        }
        car.setInventory(car.getInventory() - 1);
        carRepository.save(car);
        Rental rental = rentalMapper.toModel(requestDto);
        rental.setCar(car);
        rental.setUser(userService.getUser());
        rental.setRentalDate(LocalDate.now());
        RentalDto newRentalDto = rentalMapper.toDto(rentalRepository.save(rental));
        try {
            notificationService.send(
                    notificationMessageBuilder.buildNewRentalMessage(newRentalDto));
        } catch (RestClientException e) {
            System.err.println(e.getMessage());
        }
        return newRentalDto;
    }

    @Transactional
    @Override
    public RentalDto returnRental(Long id) throws RentalAlreadyReturnedException {
        Rental rental = rentalRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find rental with id " + id));

        User loggedUser = userService.getUser();
        if (!checkRoleIsManager(loggedUser) && !Objects.equals(rental.getUser().getId(),
                loggedUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view this rental");
        }
        if (rental.getActualReturnDate() != null) {
            throw new RentalAlreadyReturnedException("The rental with id " + id
                    + " have already finished");
        }
        rental.setActualReturnDate(LocalDate.now());
        Car car = rental.getCar();
        car.setInventory(car.getInventory() + 1);
        RentalDto newRentalDto = rentalMapper.toDto(rentalRepository.save(rental));
        try {
            notificationService.send(
                    notificationMessageBuilder.buildReturnRentalMessage(newRentalDto));
        } catch (RestClientException e) {
            System.err.println(e.getMessage());
        }
        return newRentalDto;
    }

    @Override
    public void notifyOverdueRentals() {
        List<Rental> overdueRentals =
                rentalRepository
                        .findByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate.now());
        List<RentalDto> listRentalDto = overdueRentals.stream().map(rentalMapper::toDto).toList();
        if (overdueRentals.isEmpty()) {
            try {
                notificationService.send(
                        notificationMessageBuilder.buildNoRentalsOverdueMessage());
            } catch (RestClientException e) {
                System.err.println(e.getMessage());
            }
        }
        for (RentalDto rentalDto : listRentalDto) {
            try {
                notificationService.send(
                        notificationMessageBuilder.buildOverdueRentalMessage(rentalDto));
            } catch (RestClientException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private boolean checkRoleIsManager(User loggedUser) {
        return loggedUser.getRoles()
                .stream()
                .anyMatch(role -> role.getRole() == Role.RoleName.MANAGER);
    }
}
