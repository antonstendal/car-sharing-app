package com.example.carsharing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Objects;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String field;
    private String fieldMatch;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        field = constraintAnnotation.field();
        fieldMatch = constraintAnnotation.fieldMatch();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        if (object == null) {
            return true;
        }
        try {
            Field firstField = object.getClass().getDeclaredField(field);
            firstField.setAccessible(true);
            Object firstValue = firstField.get(object);

            Field secondField = object.getClass().getDeclaredField(fieldMatch);
            secondField.setAccessible(true);
            Object secondValue = secondField.get(object);

            if (Objects.equals(firstValue, secondValue)) {
                return true;
            }
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(
                            constraintValidatorContext.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(fieldMatch)
                    .addConstraintViolation();
            return false;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field not found: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field: " + e.getMessage(), e);
        }
    }
}
