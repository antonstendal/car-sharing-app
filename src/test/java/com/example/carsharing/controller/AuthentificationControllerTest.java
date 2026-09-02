package com.example.carsharing.controller;

import com.example.carsharing.dto.user.UserLoginDto;
import com.example.carsharing.dto.user.UserLoginRequestDto;
import com.example.carsharing.dto.user.UserRegistrationRequestDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.model.Role;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = {"classpath:database/user/clean/remove-all.sql",
        "classpath:database/user/add-users-and-roles.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AuthentificationControllerTest {
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

    @Test
    @DisplayName("register() - Success: Creates new user and returns UserResponseDto")
    void register_ValidRequest_ReturnsUserResponseDto() throws Exception {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "newuser@gmai.com",
                "12345678",
                "12345678",
                "Nick",
                "Narrow"
        );

        UserResponseDto expected = new UserResponseDto(
                null,
                requestDto.email(),
                requestDto.firstName(),
                requestDto.lastName(),
                Set.of(Role.RoleName.CUSTOMER.name().toLowerCase())
        );

        String json = objectMapper.writeValueAsString(requestDto);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponseDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserResponseDto.class);

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);

        assertThat(actual.id()).isNotNull();
    }

    @Test
    @DisplayName("register() - Validation Error: Invalid request body returns 400")
    void register_InvalidRequestDto_ReturnsBadRequest() throws Exception {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "",
                "12345678",
                "12345678",
                "Nick",
                "Narrow"
        );
        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/auth/register")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andReturn();
    }

    @Test
    @DisplayName("login() - Success: Authenticates user and returns UserLoginDto")
    void login_ValidCredentials_ReturnsUserLoginDto() throws Exception {
        UserLoginRequestDto requestDto = new UserLoginRequestDto(
                "manager@gmail.com",
                "12345678");

        String json = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        UserLoginDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserLoginDto.class);

        assertThat(actual.token()).isNotBlank();
    }

    @Test
    @DisplayName("login() - Unauthorized: Invalid credentials return 401")
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        UserLoginRequestDto requestDto = new UserLoginRequestDto(
                "ghost@gmail.com",
                "55555555");
        String json = objectMapper.writeValueAsString(requestDto);
        mockMvc.perform(post("/auth/login")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }
}
