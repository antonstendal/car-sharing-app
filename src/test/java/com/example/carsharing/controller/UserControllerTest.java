package com.example.carsharing.controller;

import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.dto.user.UserUpdateRequestDto;
import com.example.carsharing.dto.user.UserUpdateRoleRequestDto;
import com.example.carsharing.model.Role;
import java.util.Set;
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
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"classpath:database/user/clean/remove-all.sql",
        "classpath:database/user/add-users-and-roles.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserControllerTest {

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

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("getLoggedUserInfo() - Success: Returns logged user info")
    void getLoggedUserInfo_ReturnsUserResponseDto() throws Exception {
        UserResponseDto expectedUser = new UserResponseDto(
                1L,
                "manager@gmail.com",
                "Bob",
                "Thornton",
                Set.of(Role.RoleName.MANAGER.name().toLowerCase())
        );

        MvcResult result = mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andReturn();

        UserResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserResponseDto.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedUser);
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("updateUserInfo() - Success: Updates logged user info")
    void updateUserInfo_ValidRequest_ReturnsUpdatedUserResponseDto() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "Sara",
                "Connor");

        UserResponseDto expected = new UserResponseDto(
                2L,
                "customer@gmail.com",
                "Sara",
                "Connor",
                Set.of(Role.RoleName.CUSTOMER.name().toLowerCase()));

        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(put("/users/me")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        UserResponseDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserResponseDto.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("updateUserInfo() - Validation Error: Invalid request body returns 400")
    void updateUserInfo_InvalidRequestDto_ReturnsBadRequest() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto("", "");

        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        MvcResult result = mockMvc.perform(put("/users/me")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("updateUserRole() - Success: Manager changes user role")
    void updateUserRole_ManagerChangesRole_ReturnsUpdatedUserResponseDto()
            throws Exception {
        UserUpdateRoleRequestDto requestDto = new UserUpdateRoleRequestDto(
                Role.RoleName.MANAGER);
        UserResponseDto expected = new UserResponseDto(
                2L,
                "customer@gmail.com",
                "Alice",
                "Harrison",
                Set.of(Role.RoleName.MANAGER.name().toLowerCase()));

        String json = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(put("/users/2/role")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        UserResponseDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserResponseDto.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
