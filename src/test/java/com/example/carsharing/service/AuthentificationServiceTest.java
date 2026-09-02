package com.example.carsharing.service;

import com.example.carsharing.dto.user.UserLoginDto;
import com.example.carsharing.dto.user.UserLoginRequestDto;
import com.example.carsharing.security.JwtUtil;
import com.example.carsharing.service.impl.AuthentificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthentificationServiceTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @InjectMocks
    private AuthentificationServiceImpl authentificationService;

    @Test
    @DisplayName("authenticate() - Success: Returns token when credentials are valid")
    void authenticate_ValidCredentials_ReturnsToken() {
        UserLoginRequestDto requestDto = new UserLoginRequestDto(
                "user@gmail.com", "12345678");

        UserLoginDto expected = new UserLoginDto("mock-token");

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(requestDto.email(), null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken(requestDto.email())).thenReturn("mock-token");

        UserLoginDto actual = authentificationService.authenticate(requestDto);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("authenticate() - Throws Exception: Invalid credentials")
    void authenticate_InvalidCredentials_ThrowsException() {
        UserLoginRequestDto requestDto = new UserLoginRequestDto(
                "ghost@gmail.com", "111");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authentificationService.authenticate(requestDto));
    }
}
