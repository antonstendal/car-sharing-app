package com.example.carsharing.service;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.rental.CreateRentalRequestDto;
import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.exception.CarOutOfStockException;
import com.example.carsharing.exception.RentalAlreadyReturnedException;
import com.example.carsharing.mapper.RentalMapper;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import com.example.carsharing.notification.RentalNotificationMessage;
import com.example.carsharing.repository.car.CarRepository;
import com.example.carsharing.repository.rental.RentalRepository;
import com.example.carsharing.service.impl.RentalServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RentalServiceTest {

    private static final BigDecimal FEE_149_50 = new BigDecimal("149.50");
    private static final BigDecimal FEE_199_99 = new BigDecimal("199.99");
    public static final String MESSAGE = "message";

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private UserService userService;
    @Mock
    private CarRepository carRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RentalNotificationMessage notificationMessageBuilder;
    @InjectMocks
    private RentalServiceImpl rentalService;

    private User mockUser(Long id, Role.RoleName roleName) {
        Role role = new Role();
        role.setRole(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@gmail.com");
        user.setPassword("12345678");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRoles(Set.of(role));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()));

        return user;
    }

    @Test
    @DisplayName("getRentals() - Success: Manager can view rentals of any user")
    void getRentals_ManagerRole_ReturnsTargetUserRentals() {
        User manager = mockUser(1L, Role.RoleName.MANAGER);

        Rental firstRental = new Rental();
        firstRental.setId(101L);
        firstRental.setRentalDate(LocalDate.of(2024, 1, 1));
        firstRental.setReturnDate(LocalDate.of(2024, 1, 5));

        Car car = new Car();
        car.setId(10L);
        car.setModel("Model S");
        car.setBrand("Tesla");
        car.setType(Car.Type.SEDAN);
        car.setInventory(5);
        car.setDailyFee(FEE_199_99);
        firstRental.setCar(car);

        RentalDto firstRentalDto = new RentalDto(
                firstRental.getId(),
                firstRental.getRentalDate(),
                firstRental.getReturnDate(),
                null,
                new CarDto(
                        car.getId(),
                        car.getModel(),
                        car.getBrand(),
                        car.getType(),
                        car.getInventory(),
                        car.getDailyFee()
                ),
                2L
        );

        Rental secondRental = new Rental();
        secondRental.setId(102L);
        secondRental.setRentalDate(LocalDate.of(2024, 5, 1));
        secondRental.setReturnDate(LocalDate.of(2024, 1, 10));
        secondRental.setCar(car);

        RentalDto secondRentalDto = new RentalDto(
                secondRental.getId(),
                secondRental.getRentalDate(),
                secondRental.getReturnDate(),
                null,
                new CarDto(
                        car.getId(),
                        car.getModel(),
                        car.getBrand(),
                        car.getType(),
                        car.getInventory(),
                        car.getDailyFee()
                ),
                3L
        );

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Rental> page = new PageImpl<>(List.of(firstRental, secondRental), pageable, 2);

        when(userService.getUser()).thenReturn(manager);
        when(rentalRepository.searchRentals(1L, true, pageable)).thenReturn(page);
        when(rentalMapper.toDto(firstRental)).thenReturn(firstRentalDto);
        when(rentalMapper.toDto(secondRental)).thenReturn(secondRentalDto);

        Page<RentalDto> actual = rentalService.getRentals(1L, true, pageable);

        assertAll(
                () -> assertThat(actual.getTotalElements()).isEqualTo(2),
                () -> assertThat(actual.getContent().get(0)).isEqualTo(firstRentalDto),
                () -> assertThat(actual.getContent().get(1)).isEqualTo(secondRentalDto)
        );

        verify(userService).getUser();
        verify(rentalRepository).searchRentals(1L, true, pageable);
        verify(rentalMapper).toDto(firstRental);
        verify(rentalMapper).toDto(secondRental);
    }

    @Test
    @DisplayName("getRentals() - Success: Customer can view only own rentals")
    void getRentals_CustomerRole_ReturnsOwnRentals() {
        User customer = mockUser(1L, Role.RoleName.CUSTOMER);

        Rental rental = new Rental();
        rental.setId(201L);
        rental.setRentalDate(LocalDate.of(2024, 1, 1));
        rental.setReturnDate(LocalDate.of(2024, 1, 5));

        Car car = new Car();
        car.setId(10L);
        car.setModel("Model S");
        car.setBrand("Tesla");
        car.setType(Car.Type.SEDAN);
        car.setInventory(5);
        car.setDailyFee(FEE_199_99);
        rental.setCar(car);

        RentalDto rentalDto = new RentalDto(
                rental.getId(),
                rental.getRentalDate(),
                rental.getReturnDate(),
                null,
                new CarDto(
                        car.getId(),
                        car.getModel(),
                        car.getBrand(),
                        car.getType(),
                        car.getInventory(),
                        car.getDailyFee()
                ),
                1L
        );

        PageRequest pageable = PageRequest.of(0, 1);
        Page<Rental> page = new PageImpl<>(List.of(rental), pageable, 1);

        when(userService.getUser()).thenReturn(customer);
        when(rentalRepository.searchRentals(1L, true, pageable)).thenReturn(page);
        when(rentalMapper.toDto(rental)).thenReturn(rentalDto);

        Page<RentalDto> actual = rentalService.getRentals(1L, true, pageable);

        assertAll(
                () -> assertThat(actual.getTotalElements()).isEqualTo(1),
                () -> assertThat(actual.getContent().get(0)).isEqualTo(rentalDto)
        );

        verify(userService).getUser();
        verify(rentalRepository).searchRentals(1L, true, pageable);
        verify(rentalMapper).toDto(rental);
    }

    @Test
    @DisplayName("getRentalById() - Success: Manager can view any rental")
    void getRentalById_ManagerRole_ReturnsRentalDto() {
        User manager = mockUser(1L, Role.RoleName.MANAGER);

        Rental rental = new Rental();
        rental.setId(300L);
        rental.setRentalDate(LocalDate.of(2024, 1, 10));
        rental.setReturnDate(LocalDate.of(2024, 1, 15));
        rental.setActualReturnDate(LocalDate.of(2024, 1, 14));

        User owner = mockUser(200L, Role.RoleName.CUSTOMER);
        rental.setUser(owner);

        Car car = new Car();
        car.setId(20L);
        car.setModel("CX-5");
        car.setBrand("Mazda");
        car.setType(Car.Type.SUV);
        car.setInventory(3);
        car.setDailyFee(FEE_149_50);
        rental.setCar(car);

        RentalDto rentalDto = new RentalDto(
                rental.getId(),
                rental.getRentalDate(),
                rental.getReturnDate(),
                rental.getActualReturnDate(),
                new CarDto(
                        car.getId(),
                        car.getModel(),
                        car.getBrand(),
                        car.getType(),
                        car.getInventory(),
                        car.getDailyFee()
                ),
                owner.getId()
        );

        when(rentalRepository.findById(300L)).thenReturn(Optional.of(rental));
        when(userService.getUser()).thenReturn(manager);
        when(rentalMapper.toDto(rental)).thenReturn(rentalDto);

        RentalDto actual = rentalService.getRentalById(300L);

        assertThat(actual).isEqualTo(rentalDto);

        verify(rentalRepository).findById(300L);
        verify(userService).getUser();
        verify(rentalMapper).toDto(rental);
    }

    @Test
    @DisplayName("getRentalById() - Throws AccessDeniedException:" +
            " Customer cannot view rental of another user")
    void getRentalById_CustomerViewingOtherUserRental_ThrowsAccessDeniedException() {
        User customer = mockUser(1L, Role.RoleName.CUSTOMER);

        Rental rental = new Rental();
        rental.setId(400L);
        rental.setRentalDate(LocalDate.of(2024, 1, 10));
        rental.setReturnDate(LocalDate.of(2024, 1, 15));
        rental.setActualReturnDate(LocalDate.of(2024, 1, 14));

        User owner = mockUser(200L, Role.RoleName.CUSTOMER);
        rental.setUser(owner);

        when(rentalRepository.findById(400L)).thenReturn(Optional.of(rental));
        when(userService.getUser()).thenReturn(customer);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> rentalService.getRentalById(400L)
        );

        assertThat(ex.getMessage()).isEqualTo(
                "You don't have permission to view this rental");

        verify(rentalRepository).findById(400L);
        verify(userService).getUser();
    }

    @Test
    @DisplayName("createRental() - " +
            "Success: Creates rental, decreases inventory and sends notification")
    void createRental_ValidRequest_ReturnsRentalDto() {
        User customer = mockUser(1L, Role.RoleName.CUSTOMER);

        CreateRentalRequestDto request = new CreateRentalRequestDto(
                50L,
                LocalDate.now().plusDays(3)
        );

        Car car = new Car();
        car.setId(50L);
        car.setModel("V50");
        car.setBrand("Volvo");
        car.setType(Car.Type.UNIVERSAL);
        car.setInventory(1);
        car.setDailyFee(new BigDecimal("59.99"));

        Rental rental = new Rental();
        rental.setId(500L);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(3));
        rental.setCar(car);

        RentalDto rentalDto = new RentalDto(
                rental.getId(),
                rental.getRentalDate(),
                rental.getReturnDate(),
                null,
                new CarDto(
                        car.getId(),
                        car.getModel(),
                        car.getBrand(),
                        car.getType(),
                        car.getInventory(),
                        car.getDailyFee()
                ),
                customer.getId()
        );

        when(carRepository.findById(50L)).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);
        when(userService.getUser()).thenReturn(customer);
        when(rentalRepository.save(any(Rental.class))).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(rentalDto);

        RentalDto actual = rentalService.createRental(request);

        assertThat(actual).isEqualTo(rentalDto);

        verify(carRepository).findById(50L);
        verify(carRepository).save(car);
        verify(userService).getUser();
        verify(rentalRepository).save(any(Rental.class));
        verify(rentalMapper).toDto(rental);
    }

    @Test
    @DisplayName("createRental() - Throws Exception: Car out of stock")
    void createRental_CarOutOfStock_ThrowsCarOutOfStockException() {
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                1L,
                LocalDate.now().plusDays(15)
        );

        Car car = new Car();
        car.setId(1L);
        car.setModel("CX-5");
        car.setBrand("Mazda");
        car.setType(Car.Type.SUV);
        car.setInventory(0);
        car.setDailyFee(FEE_149_50);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        CarOutOfStockException ex = assertThrows(
                CarOutOfStockException.class,
                () -> rentalService.createRental(request)
        );

        assertThat(ex.getMessage()).isEqualTo("Car with id 1 currently is not available");

        verify(carRepository).findById(1L);
    }

    @Test
    @DisplayName("returnRental() - " +
            "Success: Returns rental, increases inventory and sends notification")
    void returnRental_ValidRequest_ReturnsRentalDto() {
        User user = mockUser(200L, Role.RoleName.CUSTOMER);

        Rental rental = new Rental();
        rental.setId(20L);
        rental.setRentalDate(LocalDate.of(2024, 1, 10));
        rental.setReturnDate(LocalDate.of(2024, 1, 15));

        Car car = new Car();
        car.setId(20L);
        car.setModel("CX-5");
        car.setBrand("Mazda");
        car.setType(Car.Type.SUV);
        car.setInventory(3);
        car.setDailyFee(FEE_149_50);
        rental.setCar(car);

        rental.setUser(user);

        RentalDto rentalDto = new RentalDto(
                rental.getId(),
                rental.getRentalDate(),
                rental.getReturnDate(),
                null,
                new CarDto(
                        car.getId(),
                        car.getModel(),
                        car.getBrand(),
                        car.getType(),
                        car.getInventory(),
                        car.getDailyFee()
                ),
                user.getId()
        );

        when(rentalRepository.findById(20L)).thenReturn(Optional.of(rental));
        when(userService.getUser()).thenReturn(user);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(rentalDto);
        when(notificationMessageBuilder.buildReturnRentalMessage(any())).thenReturn(MESSAGE);
        doThrow(new RestClientException("fail")).when(notificationService).send(MESSAGE);

        RentalDto actual = rentalService.returnRental(20L);

        assertThat(actual).isEqualTo(rentalDto);

        verify(rentalRepository).findById(20L);
        verify(userService).getUser();
        verify(rentalRepository).save(rental);
        verify(rentalMapper).toDto(rental);
        verify(notificationMessageBuilder).buildReturnRentalMessage(actual);
        verify(notificationService).send(MESSAGE);
    }

    @Test
    @DisplayName("returnRental() - Throws RentalAlreadyReturnedException: Rental already returned")
    void returnRental_AlreadyReturned_ThrowsRentalAlreadyReturnedException() {
        User user = mockUser(200L, Role.RoleName.CUSTOMER);

        Rental rental = new Rental();
        rental.setId(756L);
        rental.setRentalDate(LocalDate.of(2024, 1, 10));
        rental.setReturnDate(LocalDate.of(2024, 1, 15));
        rental.setActualReturnDate(LocalDate.of(2024, 1, 14));
        rental.setUser(user);

        when(rentalRepository.findById(756L)).thenReturn(Optional.of(rental));
        when(userService.getUser()).thenReturn(user);

        RentalAlreadyReturnedException ex = assertThrows(
                RentalAlreadyReturnedException.class,
                () -> rentalService.returnRental(756L)
        );

        assertThat(ex.getMessage()).isEqualTo(
                "The rental with id 756 have already finished");

        verify(rentalRepository).findById(756L);
        verify(userService).getUser();
    }

    @Test
    @DisplayName("notifyOverdueRentals() - " +
            "Success: Sends overdue notifications for all overdue rentals")
    void notifyOverdueRentals_OverdueExist_SendsNotifications() {
        Rental firstRental = new Rental();
        firstRental.setId(3L);
        firstRental.setRentalDate(LocalDate.of(2026, 8, 13));
        firstRental.setReturnDate(LocalDate.of(2026, 8, 15));

        Rental secondRental = new Rental();
        secondRental.setId(4L);
        secondRental.setRentalDate(LocalDate.of(2026, 8, 13));
        secondRental.setReturnDate(LocalDate.of(2026, 8, 16));

        List<Rental> rentals = List.of(firstRental, secondRental);

        when(rentalRepository.findByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate.now()))
                .thenReturn(rentals);

        when(rentalMapper.toDto(firstRental)).thenReturn(new RentalDto(
                firstRental.getId(), firstRental.getRentalDate(),
                firstRental.getReturnDate(), null, null, 2L));
        when(rentalMapper.toDto(secondRental)).thenReturn(new RentalDto(
                secondRental.getId(), secondRental.getRentalDate(),
                secondRental.getReturnDate(), null, null, 3L));

        when(notificationMessageBuilder.buildOverdueRentalMessage(any())).thenReturn(MESSAGE);
        doNothing().when(notificationService).send(MESSAGE);

        rentalService.notifyOverdueRentals();

        verify(rentalRepository)
                .findByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate.now());
        verify(rentalMapper).toDto(firstRental);
        verify(rentalMapper).toDto(secondRental);
        verify(notificationMessageBuilder, times(2))
                .buildOverdueRentalMessage(any());
        verify(notificationService, times(2)).send(MESSAGE);
    }

    @Test
    @DisplayName("notifyOverdueRentals() - " +
            "Success: Sends 'no overdue rentals' notification when list is empty")
    void notifyOverdueRentals_NoOverdue_SendsNoOverdueNotification() {
        when(rentalRepository
                .findByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate.now()))
                .thenReturn(List.of());
        when(notificationMessageBuilder.buildNoRentalsOverdueMessage()).thenReturn(MESSAGE);
        doNothing().when(notificationService).send(MESSAGE);

        rentalService.notifyOverdueRentals();

        verify(rentalRepository)
                .findByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate.now());
        verify(notificationMessageBuilder)
                .buildNoRentalsOverdueMessage();
        verify(notificationService).send(MESSAGE);
        verifyNoMoreInteractions(rentalRepository, notificationService,
                notificationMessageBuilder);
    }
}
