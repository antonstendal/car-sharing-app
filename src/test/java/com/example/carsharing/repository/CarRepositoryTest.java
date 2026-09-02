package com.example.carsharing.repository;

import com.example.carsharing.model.Car;
import com.example.carsharing.repository.car.CarRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@ImportAutoConfiguration(classes = LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {"classpath:database/car/clean/remove-all.sql",
        "classpath:database/car/add-three-cars.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CarRepositoryTest {
    @Autowired
    private CarRepository carRepository;

    @Test
    @DisplayName("Should return matching car when model, brand and type match")
    void findByModelAndBrandAndType_carExists_returnsCar() {
        Optional<Car> actual = carRepository.findByModelAndBrandAndType(
                "Golf",
                "Volkswagen",
                Car.Type.HATCHBACK);

        assertTrue(actual.isPresent());
        assertAll(
                () -> assertEquals("Golf", actual.get().getModel()),
                () -> assertEquals("Volkswagen", actual.get().getBrand()),
                () -> assertEquals(Car.Type.HATCHBACK, actual.get().getType()));
    }

    @Test
    @DisplayName("Should return empty Optional when no car matches all conditions")
    void findByModelAndBrandAndType_carDoesNotExist_returnsEmpty() {
        Optional<Car> actual = carRepository.findByModelAndBrandAndType(
                "V50",
                "Volvo",
                Car.Type.HATCHBACK);

        assertTrue(actual.isEmpty());
    }
}
