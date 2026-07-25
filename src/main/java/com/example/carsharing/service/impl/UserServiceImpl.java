package com.example.carsharing.service.impl;

import com.example.carsharing.dto.user.UserRegistrationRequestDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.dto.user.UserUpdateRequestDto;
import com.example.carsharing.exception.DataProcessingException;
import com.example.carsharing.exception.EntityNotFoundException;
import com.example.carsharing.exception.RegistrationException;
import com.example.carsharing.mapper.UserMapper;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import com.example.carsharing.repository.user.RoleRepository;
import com.example.carsharing.repository.user.UserRepository;
import com.example.carsharing.service.UserService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.findByEmail(requestDto.email()).isPresent()) {
            throw new RegistrationException("User with email " + requestDto.email()
                    + " already exist");
        }
        User user = userMapper.toModel(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        Role role = roleRepository.findByRole(Role.RoleName.CUSTOMER).orElseThrow(
                () -> new DataProcessingException("Role "
                        + Role.RoleName.CUSTOMER + " not found"));
        user.setRoles(new HashSet<>(Set.of(role)));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getLoggedUser() {
        User user = getUser();
        return userMapper.toDto(user);
    }

    @Override
    public UserResponseDto update(UserUpdateRequestDto requestDto) {
        User loggedUser = getUser();
        User user = userRepository.findById(getUser().getId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find loggedUser by id "
                        + loggedUser.getId()));
        userMapper.update(requestDto, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto changeUserRole(Long userId, Role.RoleName roleName) {
        if (roleName == null) {
            throw new DataProcessingException("Role can't be null");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("Can't find loggedUser by id "
                        + userId));
        Role roleFromDb = roleRepository.findByRole(roleName).orElseThrow(
                () -> new EntityNotFoundException("Can't find role by value " + roleName));
        user.setRoles(new HashSet<>(Set.of(roleFromDb)));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new EntityNotFoundException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }
}
