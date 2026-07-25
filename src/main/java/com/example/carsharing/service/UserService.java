package com.example.carsharing.service;

import com.example.carsharing.dto.user.UserRegistrationRequestDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.dto.user.UserUpdateRequestDto;
import com.example.carsharing.exception.RegistrationException;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto requestDto) throws RegistrationException;

    UserResponseDto getLoggedUser();

    UserResponseDto update(UserUpdateRequestDto requestDto);

    UserResponseDto changeUserRole(Long userId, Role.RoleName roleName);

    User getUser();

}
