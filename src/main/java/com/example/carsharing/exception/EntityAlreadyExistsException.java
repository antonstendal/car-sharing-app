package com.example.carsharing.exception;

import jakarta.validation.constraints.NotBlank;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(@NotBlank(message = "Model can't be empty")
                                                String message) {
    }
}
