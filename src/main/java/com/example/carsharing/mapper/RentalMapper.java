package com.example.carsharing.mapper;

import com.example.carsharing.config.MapperConfig;
import com.example.carsharing.dto.rental.CreateRentalRequestDto;
import com.example.carsharing.dto.rental.RentalDto;
import com.example.carsharing.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class, uses = CarMapper.class)
public interface RentalMapper {
    @Mapping(source = "user.id", target = "userId")
    RentalDto toDto(Rental rental);

    @Mapping(source = "carId", target = "car.id")
    Rental toModel(CreateRentalRequestDto requestDto);
}
