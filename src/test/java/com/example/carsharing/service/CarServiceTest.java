package com.example.carsharing.service;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.car.CreateCarRequestDto;
import com.example.carsharing.dto.car.UpdateCarRequestDto;
import com.example.carsharing.exception.EntityAlreadyExistsException;
import com.example.carsharing.exception.EntityNotFoundException;
import com.example.carsharing.mapper.CarMapper;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.car.CarRepository;
import com.example.carsharing.repository.rental.RentalRepository;
import com.example.carsharing.service.impl.CarServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {
    private static final BigDecimal FEE_79_99 = new BigDecimal("79.99");
    private static final BigDecimal FEE_50_99 = new BigDecimal("50.99");
    @Mock
    private CarRepository carRepository;
    @Mock
    private CarMapper carMapper;
    @Mock
    private RentalRepository rentalRepository;
    @InjectMocks
    private CarServiceImpl carService;

    @Test
    @DisplayName("save() - Success: Valid request creates and returns new CarDto")
    void save_ValidRequestDto_ReturnsCarDto() {
        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "V50",
                "Volvo",
                Car.Type.UNIVERSAL,
                5,
                FEE_50_99);

        String model = requestDto.model();
        String brand = requestDto.brand();
        Car.Type type = requestDto.type();

        Car car = new Car();
        car.setModel(model);
        car.setBrand(brand);
        car.setType(type);
        car.setInventory(5);
        car.setDailyFee(requestDto.dailyFee());

        CarDto carDto = new CarDto(
                car.getId(),
                car.getModel(),
                car.getBrand(),
                car.getType(),
                car.getInventory(),
                car.getDailyFee());
        when(carRepository.findByModelAndBrandAndType(
                requestDto.model(),
                requestDto.brand(),
                requestDto.type())).thenReturn(Optional.empty());
        when(carMapper.toModel(requestDto)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toDto(car)).thenReturn(carDto);
        CarDto actual = carService.save(requestDto);

        assertThat(actual).isEqualTo(carDto);
        verify(carRepository).findByModelAndBrandAndType(model, brand, type);
        verify(carMapper).toModel(requestDto);
        verify(carRepository).save(car);
        verify(carMapper).toDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("save() - Throws Exception: Car with same model, brand and type already exists")
    void save_ExistingCar_ThrowsEntityAlreadyExistsException() {
        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "V50",
                "Volvo",
                Car.Type.UNIVERSAL,
                5,
                FEE_50_99);

        String model = requestDto.model();
        String brand = requestDto.brand();
        Car.Type type = requestDto.type();

        Car car = new Car();
        car.setModel(model);
        car.setBrand(brand);
        car.setType(type);
        car.setInventory(5);
        car.setDailyFee(requestDto.dailyFee());

        when(carRepository.findByModelAndBrandAndType(model, brand, type))
                .thenReturn(Optional.of(car));

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                () -> carService.save(requestDto));
        assertEquals(
                "The car with model " + model
                        + " , brand " + brand
                        + " , type " + type
                        + " already exists", exception.getMessage());
        verify(carRepository).findByModelAndBrandAndType(model, brand, type);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("findAll() - Success: Returns page of CarDto")
    void findAll_ValidPageable_ReturnsPageOfCarDto() {
        Car firstCar = new Car();
        firstCar.setId(1L);
        firstCar.setModel("Camry");
        firstCar.setBrand("Toyota");
        firstCar.setType(Car.Type.SEDAN);
        firstCar.setInventory(10);
        firstCar.setDailyFee(FEE_79_99);

        Car secondCar = new Car();
        secondCar.setId(2L);
        secondCar.setModel("Volvo");
        secondCar.setBrand("V50");
        secondCar.setType(Car.Type.UNIVERSAL);
        secondCar.setInventory(5);
        secondCar.setDailyFee(new BigDecimal("55.99"));

        CarDto firstCarDto = new CarDto(
                firstCar.getId(),
                firstCar.getModel(),
                firstCar.getBrand(),
                firstCar.getType(),
                firstCar.getInventory(),
                firstCar.getDailyFee());
        CarDto secondCarDto = new CarDto(
                secondCar.getId(),
                secondCar.getModel(),
                secondCar.getBrand(),
                secondCar.getType(),
                secondCar.getInventory(),
                secondCar.getDailyFee());

        PageRequest pageable = PageRequest.of(0, 10);
        List<Car> cars = List.of(firstCar, secondCar);
        Page<Car> carPage = new PageImpl<>(cars, pageable, cars.size());

        when(carRepository.findAll(pageable)).thenReturn(carPage);
        when(carMapper.toDto(firstCar)).thenReturn(firstCarDto);
        when(carMapper.toDto(secondCar)).thenReturn(secondCarDto);

        Page<CarDto> actual = carService.findAll(pageable);

        assertThat(actual.getContent().size()).isEqualTo(2);
        assertThat(actual.getContent().get(0)).isEqualTo(firstCarDto);
        assertThat(actual.getContent().get(1)).isEqualTo(secondCarDto);
        assertThat(actual.getTotalElements()).isEqualTo(2);
        verify(carMapper).toDto(firstCar);
        verify(carMapper).toDto(secondCar);
        verify(carRepository, times(1)).findAll(pageable);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("findAll() - Success: Returns empty page when no cars exist")
    void findAll_EmptyRepository_ReturnsEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> page = new PageImpl<>(List.of(), pageable, 0);

        when(carRepository.findAll(pageable)).thenReturn(page);

        Page<CarDto> actual = carService.findAll(pageable);

        assertThat(actual.isEmpty()).isTrue();
        assertThat(actual.getTotalElements()).isEqualTo(0);
        assertThat(actual.getContent()).isEmpty();

        verify(carRepository).findAll(pageable);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("findById() - Success: Returns CarDto for existing ID")
    void findById_ExistingId_ReturnsCarDto() {
        Car car = new Car();
        car.setId(1L);
        car.setModel("Camry");
        car.setBrand("Toyota");
        car.setType(Car.Type.SEDAN);
        car.setInventory(10);
        car.setDailyFee(FEE_79_99);

        CarDto carDto = new CarDto(
                car.getId(),
                car.getModel(),
                car.getBrand(),
                car.getType(),
                car.getInventory(),
                car.getDailyFee());

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(carDto);
        CarDto actual = carService.findById(1L);

        assertThat(actual).isEqualTo(carDto);
        assertThat(actual.brand()).isEqualTo(carDto.brand());
        assertThat(actual.model()).isEqualTo(carDto.model());
        assertThat(actual.dailyFee().compareTo(carDto.dailyFee())).isZero();
        assertThat(actual.inventory()).isEqualTo(carDto.inventory());
        assertThat(actual.type()).isEqualTo(carDto.type());
        verify(carRepository).findById(1L);
        verify(carMapper).toDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("findById() - Throws Exception: Car not found for given ID")
    void findById_NonExistingId_ThrowsEntityNotFoundException() {
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> carService.findById(999L));

        assertThat(exception.getMessage()).isEqualTo("Can't find car by id 999");
        verify(carRepository).findById(999L);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("update() - Success: Updates existing car and returns updated CarDto")
    void update_ExistingIdAndValidDto_ReturnsCarDto() {
        UpdateCarRequestDto requestDto = new UpdateCarRequestDto(
                "V50",
                "Volvo");

        Car carFromDb = new Car();
        carFromDb.setId(1L);
        carFromDb.setModel("Camry");
        carFromDb.setBrand("Toyota");
        carFromDb.setType(Car.Type.SEDAN);
        carFromDb.setInventory(10);
        carFromDb.setDailyFee(FEE_79_99);

        CarDto carDto = new CarDto(
                carFromDb.getId(),
                carFromDb.getModel(),
                carFromDb.getBrand(),
                carFromDb.getType(),
                carFromDb.getInventory(),
                carFromDb.getDailyFee());

        doAnswer(invocationOnMock -> {
            UpdateCarRequestDto request = invocationOnMock.getArgument(0);
            Car car = invocationOnMock.getArgument(1);

            car.setModel(request.model());
            car.setBrand(request.brand());
            return null;
        }).when(carMapper).update(any(), any());

        when(carRepository.findById(1L)).thenReturn(Optional.of(carFromDb));
        when(carRepository.save(carFromDb)).thenReturn(carFromDb);
        when(carMapper.toDto(carFromDb)).thenReturn(carDto);

        CarDto actual = carService.update(1L, requestDto);

        assertThat(actual).isEqualTo(carDto);
        verify(carMapper).update(requestDto, carFromDb);
        verify(carRepository).findById(1L);
        verify(carRepository).save(carFromDb);
        verify(carMapper).toDto(carFromDb);
        verifyNoMoreInteractions(carMapper, carRepository);
    }

    @Test
    @DisplayName("deleteById() - Success: Deletes car when it exists and has no active rentals")
    void deleteById_ExistingIdAndNoActiveRentals_DeletesCar() {
        // Given
        Car carFromDb = new Car();
        carFromDb.setId(1L);
        carFromDb.setModel("Camry");
        carFromDb.setBrand("Toyota");
        carFromDb.setType(Car.Type.SEDAN);
        carFromDb.setInventory(10);
        carFromDb.setDailyFee(FEE_79_99);

        when(carRepository.findById(1L)).thenReturn(Optional.of(carFromDb));
        when(rentalRepository.existsByCarIdAndActualReturnDateIsNull(1L)).thenReturn(false);

        doAnswer(invocationOnMock -> null)
                .when(carRepository).delete(carFromDb);
        carService.deleteById(1L);
        verify(carRepository).findById(1L);
        verify(rentalRepository).existsByCarIdAndActualReturnDateIsNull(1L);
        verify(carRepository).delete(carFromDb);
        verifyNoMoreInteractions(carRepository, rentalRepository);
    }

    @Test
    @DisplayName("deleteById() - Throws Exception: Cannot delete car with active rentals")
    void deleteById_ActiveRentalsExist_ThrowsEntityAlreadyExistsException() {
        Car carFromDb = new Car();
        carFromDb.setId(1L);
        carFromDb.setModel("Camry");
        carFromDb.setBrand("Toyota");
        carFromDb.setType(Car.Type.SEDAN);
        carFromDb.setInventory(10);
        carFromDb.setDailyFee(FEE_79_99);

        when(carRepository.findById(1L)).thenReturn(Optional.of(carFromDb));
        when(rentalRepository.existsByCarIdAndActualReturnDateIsNull(1L)).thenReturn(true);

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                () -> carService.deleteById(1L));

        assertThat("Cannot delete car with id 1 because it has active rentals")
                .isEqualTo(exception.getMessage());
        verify(carRepository).findById(1L);
        verify(rentalRepository).existsByCarIdAndActualReturnDateIsNull(1L);
        verifyNoMoreInteractions(carRepository, rentalRepository);
    }
}
