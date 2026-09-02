package com.example.carsharing.service;

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
import com.example.carsharing.service.impl.UserServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    public static final String PASSWORD_12345678 = "12345678";
    public static final String EMAIL = "userFromDb@gmail.com";
    public static final String FIRST_NAME_BOB = "Bob";
    public static final String LAST_NAME_MARESKA = "Mareska";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("register() - Success: Creates new user and returns UserResponseDto")
    void register_ValidRequest_ReturnsUserResponseDto() throws RegistrationException {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                EMAIL,
                PASSWORD_12345678,
                PASSWORD_12345678,
                FIRST_NAME_BOB,
                LAST_NAME_MARESKA
        );
        Role role = new Role();
        role.setRole(Role.RoleName.CUSTOMER);

        User user = new User();
        user.setId(1L);
        user.setEmail(requestDto.email());
        user.setPassword(requestDto.password());
        user.setFirstName(requestDto.firstName());
        user.setLastName(requestDto.lastName());
        user.setRoles(Set.of(role));

        UserResponseDto responseDto = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Set.of(role.getRole().name()));

        when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.empty());
        when(userMapper.toModel(requestDto)).thenReturn(user);
        when(passwordEncoder.encode(requestDto.password())).thenReturn("encoded-password");
        when(roleRepository.findByRole(Role.RoleName.CUSTOMER)).thenReturn(Optional.of(role));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(responseDto);

        UserResponseDto actual = userService.register(requestDto);

        assertThat(actual).isEqualTo(responseDto);
        verify(userRepository).findByEmail(requestDto.email());
        verify(userMapper).toModel(requestDto);
        verify(passwordEncoder).encode(requestDto.password());
        verify(roleRepository).findByRole(Role.RoleName.CUSTOMER);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
        verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder, roleRepository);
    }

    @Test
    @DisplayName("register() - Throws Exception: Email already exists")
    void register_EmailAlreadyExists_ThrowsRegistrationException() {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                EMAIL,
                PASSWORD_12345678,
                PASSWORD_12345678,
                FIRST_NAME_BOB,
                LAST_NAME_MARESKA
        );
        Role role = new Role();
        role.setRole(Role.RoleName.CUSTOMER);
        User user = new User();
        user.setId(1L);
        user.setEmail(requestDto.email());
        user.setPassword(requestDto.password());
        user.setFirstName(requestDto.firstName());
        user.setLastName(requestDto.lastName());
        user.setRoles(Set.of(role));
        when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.of(user));

        RegistrationException exception = assertThrows(RegistrationException.class,
                () -> userService.register(requestDto));

        assertThat("User with email userFromDb@gmail.com already exist")
                .isEqualTo(exception.getMessage());
    }

    @Test
    @DisplayName("getLoggedUser() - Success: Returns logged user as UserResponseDto")
    void getLoggedUser_AuthenticatedUser_ReturnsUserResponseDto() {

        Role role = new Role();
        role.setRole(Role.RoleName.CUSTOMER);

        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD_12345678);
        user.setFirstName(FIRST_NAME_BOB);
        user.setLastName(LAST_NAME_MARESKA);
        user.setRoles(Set.of(role));

        UserResponseDto responseDto = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Set.of(role.getRole().name()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        when(userMapper.toDto(user)).thenReturn(responseDto);
        UserResponseDto actual = userService.getLoggedUser();
        assertThat(actual).isEqualTo(responseDto);
        verify(userMapper).toDto(user);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    @DisplayName("update() - Success: Updates logged user and returns UserResponseDto")
    void update_ValidRequest_ReturnsUpdatedUserResponseDto() {
        Role role = new Role();
        role.setRole(Role.RoleName.CUSTOMER);

        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "Alice",
                "Kanva"
        );

        User userFromDb = new User();
        userFromDb.setId(1L);
        userFromDb.setEmail(EMAIL);
        userFromDb.setPassword(PASSWORD_12345678);
        userFromDb.setFirstName(FIRST_NAME_BOB);
        userFromDb.setLastName(LAST_NAME_MARESKA);
        userFromDb.setRoles(Set.of(role));

        UserResponseDto responseDto = new UserResponseDto(
                userFromDb.getId(),
                userFromDb.getEmail(),
                userFromDb.getFirstName(),
                userFromDb.getLastName(),
                Set.of(role.getRole().name()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                userFromDb,
                null,
                userFromDb.getAuthorities()));
        SecurityContextHolder.setContext(context);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userFromDb));

        doAnswer(i -> {
            UserUpdateRequestDto request = i.getArgument(0);
            User user = i.getArgument(1);

            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            return null;
        }).when(userMapper).update(any(), any());
        when(userRepository.save(userFromDb)).thenReturn(userFromDb);
        when(userMapper.toDto(userFromDb)).thenReturn(responseDto);

        UserResponseDto actual = userService.update(requestDto);
        assertThat(actual).isEqualTo(responseDto);
        verify(userRepository).findById(1L);
        verify(userMapper).toDto(userFromDb);
        verify(userRepository).save(userFromDb);
        verify(userMapper).update(requestDto, userFromDb);
        verifyNoMoreInteractions(userMapper, userRepository);
    }

    @Test
    @DisplayName("update() - Throws Exception: Logged user not found in repository")
    void update_LoggedUserNotFound_ThrowsEntityNotFoundException() {
        Role role = new Role();
        role.setRole(Role.RoleName.CUSTOMER);
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "Alice",
                "Kanva"
        );
        User userFromDb = new User();
        userFromDb.setId(1L);
        userFromDb.setEmail(EMAIL);
        userFromDb.setPassword(PASSWORD_12345678);
        userFromDb.setFirstName(FIRST_NAME_BOB);
        userFromDb.setLastName(LAST_NAME_MARESKA);
        userFromDb.setRoles(Set.of(role));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                userFromDb,
                null,
                userFromDb.getAuthorities()));
        SecurityContextHolder.setContext(context);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.update(requestDto));
        assertThat("Can't find loggedUser by id 1").isEqualTo(exception.getMessage());
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("changeUserRole() - Success: Changes user role and returns UserResponseDto")
    void changeUserRole_ValidRoleAndUserId_ReturnsUserResponseDto() {

        Role role = new Role();
        role.setRole(Role.RoleName.CUSTOMER);

        Role newRole = new Role();
        role.setRole(Role.RoleName.MANAGER);

        User userFromDb = new User();
        userFromDb.setId(1L);
        userFromDb.setEmail(EMAIL);
        userFromDb.setPassword(PASSWORD_12345678);
        userFromDb.setFirstName(FIRST_NAME_BOB);
        userFromDb.setLastName(LAST_NAME_MARESKA);
        userFromDb.setRoles(Set.of(role));

        UserResponseDto responseDto = new UserResponseDto(
                userFromDb.getId(),
                userFromDb.getEmail(),
                userFromDb.getFirstName(),
                userFromDb.getLastName(),
                Set.of(role.getRole().name()));

        when(userRepository.findById(1L)).thenReturn(Optional.of(userFromDb));
        when(roleRepository.findByRole(Role.RoleName.MANAGER)).thenReturn(Optional.of(newRole));
        when(userRepository.save(userFromDb)).thenReturn(userFromDb);
        when(userMapper.toDto(userFromDb)).thenReturn(responseDto);

        UserResponseDto actual = userService.changeUserRole(1L, Role.RoleName.MANAGER);
        assertThat(actual).isEqualTo(responseDto);
        verify(userRepository).findById(1L);
        verify(roleRepository).findByRole(Role.RoleName.MANAGER);
        verify(userRepository).save(userFromDb);
        verify(userMapper).toDto(userFromDb);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("changeUserRole() - Throws DataProcessingException: RoleName is null")
    void changeUserRole_NullRole_ThrowsDataProcessingException() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
                () -> userService.changeUserRole(1L, null));
        assertThat("Role can't be null").isEqualTo(exception.getMessage());
    }

    @Test
    @DisplayName("changeUserRole() - Throws EntityNotFoundException: Role not found in database")
    void changeUserRole_RoleNotFound_ThrowsEntityNotFoundException() {
        Role role = new Role();
        role.setRole(Role.RoleName.MANAGER);

        User userFromDb = new User();
        userFromDb.setId(1L);
        userFromDb.setEmail(EMAIL);
        userFromDb.setPassword(PASSWORD_12345678);
        userFromDb.setFirstName(FIRST_NAME_BOB);
        userFromDb.setLastName(LAST_NAME_MARESKA);
        userFromDb.setRoles(Set.of(role));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userFromDb));
        when(roleRepository.findByRole(role.getRole())).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.changeUserRole(1L, role.getRole()));
        assertEquals("Can't find role by value MANAGER", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(roleRepository).findByRole(role.getRole());
        verifyNoMoreInteractions(roleRepository, userRepository);
    }
}
