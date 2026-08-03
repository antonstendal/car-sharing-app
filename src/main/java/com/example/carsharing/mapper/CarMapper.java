package com.example.carsharing.mapper;

import com.example.carsharing.config.MapperConfig;
import com.example.carsharing.dto.car.CarDto;
import com.example.carsharing.dto.car.CreateCarRequestDto;
import com.example.carsharing.dto.car.UpdateCarRequestDto;
import com.example.carsharing.model.Car;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface CarMapper {
    CarDto toDto(Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Car toModel(CreateCarRequestDto requestDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "dailyFee", ignore = true)
    void update(UpdateCarRequestDto requestDto, @MappingTarget Car car);
}
