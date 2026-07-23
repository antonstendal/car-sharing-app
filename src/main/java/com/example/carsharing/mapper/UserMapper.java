package com.example.carsharing.mapper;

import com.example.carsharing.config.MapperConfig;
import com.example.carsharing.dto.user.UserRegistrationRequestDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.dto.user.UserUpdateRequestDto;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToString")
    UserResponseDto toDto(User user);

    User toModel(UserRegistrationRequestDto requestDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(UserUpdateRequestDto requestDto, @MappingTarget User user);

    @Named("mapRolesToString")
    default Set<String> mapRolesToString(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(role -> role.getRole().name())
                .collect(Collectors.toSet());
    }
}
