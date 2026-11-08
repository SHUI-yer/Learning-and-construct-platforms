package com.learning.lpauth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankLoginCredentials() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(" ");
        dto.setPassword("");

        assertTrue(validator.validate(dto).size() >= 2);
    }

    @Test
    void rejectsWeakResetPasswordAndInvalidUserId() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setUserId(0L);
        dto.setNewPassword("short");

        assertTrue(validator.validate(dto).size() >= 2);
    }
}
