package com.example.carsharing.service;

import com.example.carsharing.dto.user.UserLoginDto;
import com.example.carsharing.dto.user.UserLoginRequestDto;

public interface AuthentificationService {
    UserLoginDto authenticate(UserLoginRequestDto requestDto);
}
