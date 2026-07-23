package com.example.carsharing.service.impl;

import com.example.carsharing.dto.user.UserLoginDto;
import com.example.carsharing.dto.user.UserLoginRequestDto;
import com.example.carsharing.security.JwtUtil;
import com.example.carsharing.service.AuthentificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthentificationServiceImpl implements AuthentificationService {
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserLoginDto authenticate(UserLoginRequestDto requestDto) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password()));
        String token = jwtUtil.generateToken(authentication.getName());
        return new UserLoginDto(token);
    }
}
