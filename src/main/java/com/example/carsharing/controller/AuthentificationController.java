package com.example.carsharing.controller;

import com.example.carsharing.dto.user.UserLoginDto;
import com.example.carsharing.dto.user.UserLoginRequestDto;
import com.example.carsharing.dto.user.UserRegistrationRequestDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.exception.RegistrationException;
import com.example.carsharing.service.AuthentificationService;
import com.example.carsharing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Registration and login management", description = "Endpoints for authentication users")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthentificationController {
    private final AuthentificationService authentificationService;
    private final UserService userService;

    @Operation(summary = "Registration", description = "New user registration")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody @Valid UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        return userService.register(requestDto);
    }

    @Operation(summary = "Login", description = "Login user")
    @PostMapping("/login")
    public UserLoginDto login(@RequestBody @Valid UserLoginRequestDto requestDto) {
        return authentificationService.authenticate(requestDto);
    }
}
