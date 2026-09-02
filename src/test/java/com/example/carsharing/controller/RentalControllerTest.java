package com.example.carsharing.controller;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.rental.CreateRentalRequestDto;
import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.model.Car;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"classpath:database/rental/clean/remove-all-rentals.sql",
        "classpath:database/rental/add-rentals.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RentalControllerTest {
    private static final BigDecimal FEE_199_99 = new BigDecimal("199.99");
    private static final BigDecimal FEE_149_50 = new BigDecimal("149.50");

    @Autowired
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(
            @Autowired WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity()).build();
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("create() - Success: Creates new rental and returns RentalDto")
    void create_ValidRequest_ReturnsRentalDto() throws Exception {
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                2L,
                LocalDate.now().plusDays(3)
        );
        RentalDto expected = new RentalDto(
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                null,
                new CarDto(
                        2L,
                        "CX-5",
                        "Mazda",
                        Car.Type.SUV,
                        2,
                        FEE_149_50),
                2L);

        String json = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(post("/rentals")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        RentalDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                RentalDto.class);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Sql(scripts = {"classpath:database/car/clean/remove-all.sql",
            "classpath:database/car/add-car-with-zero-inventory.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @DisplayName("create() - Error: Car out of stock returns 409")
    void create_CarOutOfStock_ThrowsException() throws Exception {
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                1L,
                LocalDate.now().plusDays(3)
        );
        RentalDto expected = new RentalDto(
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                null,
                new CarDto(
                        1L,
                        "CX-5",
                        "Mazda",
                        Car.Type.SUV,
                        2,
                        FEE_149_50),
                2L);

        String json = objectMapper.writeValueAsString(requestDto);
        mockMvc.perform(post("/rentals")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("getAll() - Success: Returns paginated list of rentals")
    void getAll_ReturnsPageOfRentals() throws Exception {
        CarDto tesla = new CarDto(
                1L,
                "Model S",
                "Tesla",
                Car.Type.SEDAN,
                5,
                FEE_199_99);

        CarDto mazda = new CarDto(
                2L,
                "CX-5",
                "Mazda",
                Car.Type.SUV,
                3,
                FEE_149_50);

        List<RentalDto> expected = List.of(
                new RentalDto(
                        1L,
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 1, 5),
                        null,
                        tesla,
                        1L
                ),
                new RentalDto(
                        2L,
                        LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 1, 15),
                        LocalDate.of(2024, 1, 14),
                        tesla,
                        1L
                ),
                new RentalDto(
                        3L,
                        LocalDate.of(2024, 1, 2),
                        LocalDate.of(2024, 1, 6),
                        null,
                        mazda,
                        2L
                ),
                new RentalDto(
                        4L,
                        LocalDate.of(2024, 1, 7),
                        LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 1, 9),
                        mazda,
                        2L
                )
        );

        MvcResult result = mockMvc.perform(get("/rentals"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);

        List<RentalDto> actual = objectMapper.readValue(
                root.get("content").toString(),
                new TypeReference<>() {
                });
        assertThat(actual).usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("getAll() - Forbidden: Unauthorized user gets 403")
    void getAll_ForbiddenForUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/rentals"))
                .andExpect(status().isUnauthorized())
                .andReturn();

    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("getById() - Success: Returns rental by ID")
    void getById_ValidId_ReturnsRentalDto() throws Exception {
        RentalDto expected = new RentalDto(
                1L,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 5),
                null,
                new CarDto(
                        1L,
                        "Model S",
                        "Tesla",
                        Car.Type.SEDAN,
                        5,
                        FEE_199_99),
                1L);
        MvcResult result = mockMvc.perform(get("/rentals/1"))
                .andExpect(status().isOk())
                .andReturn();

        RentalDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                RentalDto.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("getById() - Not Found: Rental with given ID does not exist")
    void getById_RentalNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/rentals/999"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("rentalReturn() - Success: Returns rental with updated return date")
    void rentalReturn_ValidId_ReturnsUpdatedRentalDto() throws Exception {
        RentalDto expected = new RentalDto(
                3L,
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 6),
                LocalDate.now(),
                new CarDto(
                        2L,
                        "CX-5",
                        "Mazda",
                        Car.Type.SUV,
                        4,
                        FEE_149_50),
                2L);

        MvcResult result = mockMvc.perform(post("/rentals/3/return"))
                .andExpect(status().isOk())
                .andReturn();

        RentalDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                RentalDto.class);

        assertThat(actual).usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("rentalReturn() - Error: Rental already returned throws exception")
    void rentalReturn_AlreadyReturned_ThrowsException() throws Exception {

        MvcResult result = mockMvc.perform(post("/rentals/4/return"))
                .andDo(print())
                .andExpect(status().isConflict())
                .andReturn();
    }

}
