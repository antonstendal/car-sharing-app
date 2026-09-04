package com.example.carsharing.mapper;

import com.example.carsharing.config.MapperConfig;
import com.example.carsharing.dto.payment.PaymentDto;
import com.example.carsharing.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {
    @Mapping(source = "payment.id", target = "rentalId")
    PaymentDto toDto(Payment payment);
}
