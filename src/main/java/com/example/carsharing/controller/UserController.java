package com.example.carsharing.controller;

import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.dto.user.UserUpdateRequestDto;
import com.example.carsharing.dto.user.UserUpdateRoleRequestDto;
import com.example.carsharing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users managing", description = "Endpoints for handling authenticated users")
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Get logged user", description = "Return info about logged user")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    @GetMapping("/me")
    public UserResponseDto getLoggedUserInfo() {
        return userService.getLoggedUser();
    }

    @Operation(summary = "Update logged user info", description = "Update logged user data")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    @PutMapping("/me")
    public UserResponseDto updateUserInfo(@RequestBody @Valid UserUpdateRequestDto requestDto) {
        return userService.update(requestDto);
    }

    @Operation(summary = "Change user role", description = "Change user role")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}/role")
    public UserResponseDto updateUserRole(@PathVariable Long id,
                                          @RequestBody @Valid UserUpdateRoleRequestDto requestDto) {
        return userService.changeUserRole(id, requestDto.roleName());
    }
}
