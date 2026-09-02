package com.example.carsharing.controller;

import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.car.CreateCarRequestDto;
import com.example.carsharing.dto.car.UpdateCarRequestDto;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.car.CarRepository;
import java.math.BigDecimal;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"classpath:database/car/clean/remove-all.sql",
        "classpath:database/user/clean/remove-all.sql",
        "classpath:database/user/add-users-and-roles.sql",
        "classpath:database/car/add-three-cars.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CarControllerTest {

    @Autowired
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @BeforeAll
    static void beforeAll(
            @Autowired WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity()).build();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("create() - Success: Manager creates a new car")
    void create_ManagerCreatesCar_ReturnsCreatedCarDto() throws Exception {

        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "Vectra",
                "Opel",
                Car.Type.SEDAN,
                2,
                new BigDecimal("20.99"));

        CarDto expected = new CarDto(
                4L,
                requestDto.model(),
                requestDto.brand(),
                requestDto.type(),
                requestDto.inventory(),
                requestDto.dailyFee());

        String json = objectMapper.writeValueAsString(requestDto);

        MvcResult result = mockMvc.perform(post("/cars")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        CarDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                CarDto.class);

        assertNotNull(actual);
        assertNotNull(actual.id());
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("create() - Validation Error: Invalid request body returns 400")
    void create_InvalidRequestDto_ReturnsBadRequest() throws Exception {
        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "Vectra",
                "",
                Car.Type.SEDAN,
                2,
                new BigDecimal("20.99"));

        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/cars")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.brand").exists())
                .andReturn();
    }

    @Test
    @DisplayName("findAll() - Success: Returns paginated list of cars")
    void findAll_ReturnsPageOfCars() throws Exception {
        List<CarDto> expected = List.of(
                new CarDto(
                        1L,
                        "Model S",
                        "Tesla",
                        Car.Type.SEDAN,
                        5,
                        BigDecimal.valueOf(199.99)
                ),
                new CarDto(
                        2L,
                        "CX-5",
                        "Mazda",
                        Car.Type.SUV,
                        3,
                        BigDecimal.valueOf(149.50)
                ),
                new CarDto(
                        3L,
                        "Golf",
                        "Volkswagen",
                        Car.Type.HATCHBACK,
                        7,
                        BigDecimal.valueOf(99.00)
                ));

        MvcResult result = mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(content);

        List<CarDto> actual = objectMapper.readValue(
                root.get("content").toString(),
                new TypeReference<>() {
                });

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Sql(scripts = "classpath:database/car/clean/remove-all.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @DisplayName("findAll() - Success: Returns empty page when no cars exist")
    void findAll_ReturnsEmptyPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);

        List<CarDto> actual = objectMapper.readValue(
                root.get("content").toString(),
                new TypeReference<>() {
                });

        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("findById() - Success: Returns car by ID")
    void findById_ValidId_ReturnsCarDto() throws Exception {
        CarDto expected = new CarDto(
                1L,
                "Model S",
                "Tesla",
                Car.Type.SEDAN,
                5,
                BigDecimal.valueOf(199.99)
        );

        MvcResult result = mockMvc.perform(get("/cars/1"))
                .andExpect(status().isOk())
                .andReturn();
        CarDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                CarDto.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("findById() - Not Found: Car with given ID does not exist")
    void findById_CarNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/cars/999"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("update() - Success: Manager updates car")
    void update_ManagerUpdatesCar_ReturnsUpdatedCarDto() throws Exception {
        UpdateCarRequestDto requestDto = new UpdateCarRequestDto(
                "Vectra",
                "Opel");
        CarDto expected = new CarDto(
                2L,
                requestDto.model(),
                requestDto.brand(),
                Car.Type.SUV,
                3,
                new BigDecimal("149.50"));

        String json = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(put("/cars/2")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        CarDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                CarDto.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("update() - Validation Error: Invalid update request returns 400")
    void update_InvalidRequestDto_ReturnsBadRequest() throws Exception {
        UpdateCarRequestDto requestDto = new UpdateCarRequestDto(
                "",
                "Opel");
        String json = objectMapper.writeValueAsString(requestDto);
        mockMvc.perform(put("/cars/2")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.model").exists())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("delete() - Success: Manager deletes car and returns 204")
    void delete_ManagerDeletesCar_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/cars/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Content-Type"));

        assertTrue(carRepository.findById(1L).isEmpty());
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("delete() - Not Found: Car with given ID does not exist")
    void delete_CarNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/cars/999"))
                .andExpect(status().isNotFound())
                .andReturn();
        assertTrue(carRepository.findById(999L).isEmpty());
    }
}
